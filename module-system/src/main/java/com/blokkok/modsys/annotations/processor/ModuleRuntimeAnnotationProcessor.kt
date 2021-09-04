package com.blokkok.modsys.annotations.processor

import android.annotation.SuppressLint
import android.os.Build
import com.blokkok.modsys.ModuleLoader
import com.blokkok.modsys.communication.Communication
import com.blokkok.modsys.communication.ExtensionPointCommunication
import com.blokkok.modsys.communication.FunctionCommunication
import com.blokkok.modsys.communication.namespace.NamespaceResolver
import com.blokkok.modsys.modinter.Module
import com.blokkok.modsys.modinter.annotations.ExtensionPoint
import com.blokkok.modsys.modinter.annotations.Function
import com.blokkok.modsys.modinter.annotations.ImplementsExtensionPoint
import com.blokkok.modsys.modinter.annotations.Namespace
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.jvmErasure

import com.blokkok.modsys.communication.namespace.Namespace as NamespaceComm

/**
 * This class processes annotations of a module and returns a list of communications defined using
 * annotations by the module given
 */
object ModuleRuntimeAnnotationProcessor {

    @Throws(UnsupportedOperationException::class, AnnotationProcessingException::class)
    fun process(
        moduleInst: Module,
        moduleClass: Class<out Module>,
        communications: HashMap<String, Communication>,
    ) {
        // check if this class is made in kotlin (this will be used to check for optional params and
        // parameter names if this class was made in kotlin)
        val isKotlin = moduleClass.getAnnotation(Metadata::class.java) != null

        // then process it, depending on how the module is made in java or kotlin
        if (isKotlin) {
            KotlinProcessor.process(moduleInst, communications)
        } else {
            JavaProcessor.process(moduleInst, communications)
        }
    }

    /**
     * Runtime annotation processor for kotlin-made modules
     */
    private object KotlinProcessor {
        @Throws(AnnotationProcessingException::class)
        fun process(
            instance: Any,
            communications: HashMap<String, Communication>,
            parentNamespace: NamespaceComm? = null,
        ) {
            // Process functions ===================================================================

            // alright, since kotlin has optional parameters, name parameters, etc, it's going to be
            // different and easier
            val instanceClass = instance::class

            // loop through each functions of this class
            for (member in instanceClass.memberFunctions) {
                val funcAnnotation = member.findAnnotation<Function>() ?: continue

                processFunc(funcAnnotation, member, communications, instance)
            }

            // Process objects / namespaces (and extension points) =================================

            // and also loop through each subclasses of this class to find something like
            // namespaces, extension points, and extension point implementors
            for (member in instanceClass.nestedClasses) {
                processObject(member, communications, parentNamespace)
            }
        }

        @Throws(AnnotationProcessingException::class)
        private fun processFunc(
            funcAnnotation: Function,
            member: KFunction<*>,
            communications: HashMap<String, Communication>,
            instance: Any,
        ) {
            val funcName = if (funcAnnotation.name == "") member.name else funcAnnotation.name

            // map this function's parameters into required and optional hashmaps
            val requiredParams = HashMap<KParameter, Class<*>>()
            val optionalParams = HashMap<KParameter, Class<*>>()

            member.parameters.forEach {
                val params = if (it.isOptional) optionalParams else requiredParams
                params[it] = it.type.jvmErasure.java
            }

            // then create a function communication based on the name
            putResult(communications, funcName, FunctionCommunication { funcCallArgs ->
                // do type checks as well as mapping the parameters
                val args = HashMap(requiredParams.keys.associateWith { param ->
                    // kotlin has this instance parameter where you have to pass in the instance
                    // where you wanted the method to be called
                    if (param.kind == KParameter.Kind.INSTANCE)
                        return@associateWith instance

                    val arg = funcCallArgs[param.name]
                        ?: throw IllegalArgumentException(
                            "Argument ${param.name} must be present"
                        )

                    if (!arg.javaClass.canBePassedTo(param.type.jvmErasure.java))
                        throw ClassCastException(
                            "The type of the argument of \"${param.name}\" " +
                            "(${arg.javaClass.name}) cannot be fed with the type of the passed " +
                            "argument \"${param.type.jvmErasure.java.name}\""
                        )

                    return@associateWith funcCallArgs[param.name]
                })

                // add optional params if they're passed
                for (param in optionalParams.keys) {
                    // get the param, if it exists. if it doesn't then just skip
                    val arg: Any = funcCallArgs[param.name] ?: continue

                    // type check!
                    if (!arg.javaClass.canBePassedTo(param.type.jvmErasure.java))
                        throw ClassCastException(
                            "The type of the optional parameter of \"${param.name}\" " +
                            "(${arg.javaClass.name}) cannot be fed with the type of the passed " +
                            "argument \"${param.type.jvmErasure.java.name}\""
                        )

                    // alright, we're good! add it to the args
                    args[param] = arg
                }

                // alright, les go call the function!
                member.callBy(args)
            })
        }

        @Throws(AnnotationProcessingException::class)
        private fun processObject(
            member: KClass<*>,
            communications: HashMap<String, Communication>,
            parentNamespace: NamespaceComm?,
        ) {
            // check if this class is an object class
            if (member.objectInstance != null) {
                // check for either if this has Namespace or ImplementsExtensionPoint
                if (member.hasAnnotation<Namespace>()) {
                    val annotation = member.findAnnotation<Namespace>() ?: return
                    val namespaceName =
                        if (annotation.name == "") member.simpleName!! else annotation.name

                    val namespace = NamespaceComm(
                        namespaceName,
                        HashMap(),
                        parentNamespace
                    )

                    // ok, now that we've got the namespace info, let's parse its communications by
                    // recursively calling process()
                    val nmCommunications = HashMap<String, Communication>()
                    process(member.objectInstance!!, nmCommunications, namespace)

                    // add all of those communications
                    namespace.communications.putAll(nmCommunications)

                    // now add the new namespace to our result
                    putResult(nmCommunications, namespaceName, namespace)

                } else if (member.hasAnnotation<ImplementsExtensionPoint>()) {
                    val annotation = member.findAnnotation<ImplementsExtensionPoint>() ?: return

                    val namespace = NamespaceResolver.resolveNamespace(annotation.extPointNamespace)
                        ?: throw AnnotationProcessingException(
                            "Failed to resolve namespace ${annotation.extPointNamespace} when " +
                            "trying to process an extension point implementor " +
                            "\"${annotation.extPointNamespace}/${annotation.extPointName}\""
                        )

                    val extPoint = namespace.communications[annotation.extPointName]
                    if (extPoint !is ExtensionPointCommunication)
                        throw AnnotationProcessingException(
                            "The communication pointed is not an ExtensionPoint while trying to " +
                            "resolve an extension point implementor " +
                            "\"${annotation.extPointNamespace}/${annotation.extPointName}\""
                        )

                    // Map the methods from the ExtensionPoint's methods spec (interface)
                    // to this implementor's methods
                    val mappedMethods = HashMap<MethodSpec, Method>().apply {
                        extPoint.spec.forEach { it ->
                            val spec = it.key

                            put(
                                spec,
                                member.declaredMemberFunctions.find {
                                    MethodSpec.fromMethod(it.javaMethod!!) == spec
                                }?.javaMethod
                                    ?: throw AnnotationProcessingException(
                                        "Cannot find a method in the implementor of an extension " +
                                        "point ${member.qualifiedName} with the spec $spec"
                                    )
                            )
                        }
                    }

                    // and we can create the proxy calls that uses the mapped methods to determine
                    // which function should it call
                    extPoint.implementors.add(
                        Proxy.newProxyInstance(
                            ModuleLoader.moduleClassLoader.loader,
                            arrayOf(extPoint.specClass)
                        ) { _, method, args ->
                            if (args == null) {
                                mappedMethods[
                                        MethodSpec.fromMethod(method)
                                ]!!.invoke(member.objectInstance!!)
                            } else {
                                mappedMethods[
                                        MethodSpec.fromMethod(method)
                                ]!!.invoke(member.objectInstance!!, *args)
                            }
                        }
                    )
                }

            } else if (member.hasAnnotation<ExtensionPoint>()) {
                if (!member.java.isInterface)
                    throw AnnotationProcessingException(
                        "Class ${member.qualifiedName} is annotated as an extension point. But " +
                        "its not an interface. Only interfaces can become an extension point"
                    )

                val annotation = member.findAnnotation<ExtensionPoint>() ?: return
                val extPointName =
                    (if (annotation.name.isEmpty()) member.simpleName else annotation.name)!!

                putResult(
                    communications,
                    extPointName,
                    ExtensionPointCommunication(
                        member.declaredMemberFunctions.associate {
                            val method = it.javaMethod!!
                            Pair(MethodSpec.fromMethod(method), method)
                        },
                        member.java
                    )
                )
            }
        }
    }

    /**
     * Runtime annotation processor for java-made modules
     */
    private object JavaProcessor {
        @Throws(UnsupportedOperationException::class, AnnotationProcessingException::class)
        fun process(
            instance: Any,
            communications: HashMap<String, Communication>,
            parentNamespace: NamespaceComm? = null
        ) {
            val instanceClass = instance::class.java

            // loop for each properties in this class and check if the Function or Namespace
            // annotation is present
            for (method in instanceClass.declaredMethods) {
                val funcAnnotation = method.getAnnotation(Function::class.java) ?: continue
                processFunc(funcAnnotation, method, communications, instance)
            }

            // now let's parse its namespaces, since java doesn't have object like kotlin does,
            // java-made modules' functions inside a class can be invoked in two ways:
            //  - static
            //  - no-arg constructor
            // currently, this only supports no-arg constr sadly
            // TODO: 8/27/21 static methods as func comm
            for (clazz in instanceClass.classes) {
                val namespaceAnnotation = clazz.getAnnotation(Namespace::class.java) ?: continue
                processClass(namespaceAnnotation, clazz, communications, parentNamespace)
            }
        }

        @SuppressLint("NewApi") // <- false positive
        @Throws(UnsupportedOperationException::class, AnnotationProcessingException::class)
        private fun processFunc(
            funcAnnotation: Function,
            method: Method,
            communications: HashMap<String, Communication>,
            moduleInst: Any,
        ) {
            val funcName = if (funcAnnotation.name == "") method.name else funcAnnotation.name

            // since java doesn't support parameter names like kotlin does, we will need to map
            // the parameter names into its parameter index and its type

            val params = if (funcAnnotation.paramNames.isEmpty()) {
                // sadly, this can only be done on API level 26+
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                    throw UnsupportedOperationException()

                method.parameters.associate { Pair(it.name, it.type) }
            } else {
                // check if the given mapping is correct
                if (funcAnnotation.paramNames.size != method.parameterCount)
                    throw IllegalArgumentException(
                        "paramNames given doesn't have the same length " +
                        "(${funcAnnotation.paramNames.size}) as the method" +
                        " (${method.parameterCount}). Method name: ${method.name}"
                    )

                HashMap<String, Class<*>>().apply {
                    method.parameterTypes.forEachIndexed { index, clazz ->
                        put(funcAnnotation.paramNames[index], clazz)
                    }
                }
            }

            putResult(communications, funcName, FunctionCommunication { passedArgs ->
                // do type checks as well as mapping the arguments
                val args = params.map { param ->
                    val arg = passedArgs[param.key]
                        ?: throw IllegalArgumentException(
                            "Argument ${param.key} must be present"
                        )

                    // type check!
                    if (!arg.javaClass.canBePassedTo(param.value))
                        throw ClassCastException(
                            "The type of the argument of \"${param.key}\" " +
                            "(${arg.javaClass.name}) cannot be fed with the type of the passed " +
                            "argument \"${param.value.name}\""
                        )

                    return@map param.value
                }.toTypedArray()

                // alright, les go invoke the function!
                method.invoke(moduleInst, *args)
            })
        }

        @Throws(AnnotationProcessingException::class)
        private fun processClass(
            namespaceAnnotation: Namespace,
            clazz: Class<*>,
            communications: HashMap<String, Communication>,
            parentNamespace: NamespaceComm?,
        ) {
            val namespaceName =
                if (namespaceAnnotation.name == "") clazz.simpleName else namespaceAnnotation.name

            val namespace = NamespaceComm(
                namespaceName,
                HashMap(),
                parentNamespace
            )

            // we need to instantiate this class
            val clazzInst = clazz.newInstance()

            // then call process() to process its communications
            val nmCommunications = HashMap<String, Communication>()
            process(clazzInst, nmCommunications, namespace)

            // put all the communications
            namespace.communications.putAll(nmCommunications)

            // finally put it on the results
            putResult(communications, namespaceName, namespace)
        }
    }

    @Throws(AnnotationProcessingException::class)
    private fun putResult(
        result: HashMap<String, Communication>,
        name: String,
        value: Communication,
    ) {
        if (result.containsKey(name))
            throw AnnotationProcessingException(
                "the same communication name \"$name\" already exists in the current namespace. " +
                "Trying to put the communication ${value.name} with the name \"$name\" but " +
                "${result[name]!!.name} is already there."
            )

        result[name] = value
    }
}

/**
 * Used to check if a supposed class's instance can be passed into the specified [clazz]
 */
private fun Class<*>.canBePassedTo(clazz: Class<*>): Boolean {
    // first, let's do a simple check
    if (this == clazz) return true

    // and use java's isAssignableFrom
    if (this.isAssignableFrom(clazz)) return true

    // if not then check if this class or the clazz is a primitive value
    if (this.isPrimitive) {
        // ok, check if clazz is also a primitive value
        if (clazz.isPrimitive) return true

        // if not then check if clazz has a primitive value
        clazz.kotlin.javaPrimitiveType?.let {
            // this has a primitive type! do the type checking!
            return this.canBePassedTo(it)
        }

        // if it doesn't then it can't be passed to
    } else if (clazz.isPrimitive) {
        // ok, check if clazz is also a primitive value
        if (this.isPrimitive) return true

        // if not then check if clazz has a primitive value
        this.kotlin.javaPrimitiveType?.let {
            // this has a primitive type! do the type checking!
            return it.canBePassedTo(clazz)
        }

        // if it doesn't then it can't be passed to
    }

    return false
}