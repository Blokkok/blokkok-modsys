package com.blokkok.modsys

import android.annotation.SuppressLint
import android.os.Build
import com.blokkok.modsys.communication.Communication
import com.blokkok.modsys.communication.FunctionCommunication
import com.blokkok.modsys.modinter.Module
import com.blokkok.modsys.modinter.annotations.Function
import com.blokkok.modsys.modinter.annotations.Namespace
import java.lang.reflect.Method
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.jvmErasure

import com.blokkok.modsys.communication.namespace.Namespace as NamespaceComm

/**
 * This class processes annotations of a module and returns a list of communications defined using
 * annotations by the module given
 */
object ModuleRuntimeAnnotationProcessor {

    @Throws(UnsupportedOperationException::class)
    fun process(
        moduleInst: Module,
        moduleClass: Class<out Module>,
    ): Map<String, Communication> {

        // check if this class is made in kotlin (this will be used to check for optional params and
        // parameter names if this class was made in kotlin)
        val isKotlin = moduleClass.getAnnotation(Metadata::class.java) != null

        // then process it, depending on how the module is made in java or kotlin
        return if (isKotlin) {
            KotlinProcessor.process(moduleInst)
        } else {
            JavaProcessor.process(moduleInst)
        }
    }

    /**
     * Runtime annotation processor for kotlin-made modules
     */
    private object KotlinProcessor {
        fun process(
            instance: Any,
            parentNamespace: NamespaceComm? = null
        ): Map<String, Communication> {
            val result = HashMap<String, Communication>()

            // Process functions ===================================================================

            // alright, since kotlin has optional parameters, name parameters, etc, it's going to be
            // different and easier
            val instanceClass = instance::class

            // loop through each functions of this class
            for (member in instanceClass.memberFunctions) {
                val funcAnnotation = member.findAnnotation<Function>() ?: continue

                processFunc(funcAnnotation, member, result, instance)
            }

            // Process objects / namespaces ========================================================

            // and also loop through each classes of this class, we're searching for an object class
            // that contains the @Namespace annotation
            for (member in instanceClass.nestedClasses) {
                // check if this class is an object & has the namespace annotation, if not then continue
                if (member.objectInstance == null) continue
                val annotation = member.findAnnotation<Namespace>() ?: continue

                processObject(member, result, parentNamespace, annotation)
            }

            return result
        }

        private fun processFunc(
            funcAnnotation: Function,
            member: KFunction<*>,
            result: HashMap<String, Communication>,
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
            result[funcName] = FunctionCommunication { funcCallArgs ->
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
            }
        }

        private fun processObject(
            member: KClass<*>,
            result: HashMap<String, Communication>,
            parentNamespace: NamespaceComm?,
            annotation: Namespace,
        ) {
            val namespaceName = if (annotation.name == "") member.simpleName!! else annotation.name
            val namespace = NamespaceComm(
                namespaceName,
                HashMap(),
                parentNamespace
            )

            // ok, now that we've got the namespace info, let's parse its communications by
            // recursively calling process()
            val communications = process(member.objectInstance!!, namespace)

            // add all of those communications
            namespace.communications.putAll(communications)

            // now add the new namespace to our result
            result[namespaceName] = namespace
        }
    }

    /**
     * Runtime annotation processor for java-made modules
     */
    private object JavaProcessor {
        @Throws(UnsupportedOperationException::class)
        fun process(
            instance: Any,
            parentNamespace: NamespaceComm? = null
        ): Map<String, Communication> {
            val result = HashMap<String, Communication>()
            val instanceClass = instance::class.java

            // loop for each properties in this class and check if the Function or Namespace
            // annotation is present
            for (method in instanceClass.declaredMethods) {
                val funcAnnotation = method.getAnnotation(Function::class.java) ?: continue
                processFunc(funcAnnotation, method, result, instance)
            }

            // now let's parse its namespaces, since java doesn't have object like kotlin does,
            // java-made modules' functions inside a class can be invoked in two ways:
            //  - static
            //  - no-arg constructor
            // currently, this only supports no-arg constr sadly
            // TODO: 8/27/21 static methods as func comm
            for (clazz in instanceClass.classes) {
                val namespaceAnnotation = clazz.getAnnotation(Namespace::class.java) ?: continue
                processClass(namespaceAnnotation, clazz, result, parentNamespace)
            }

            return result
        }

        @SuppressLint("NewApi") // <- false positive
        @Throws(UnsupportedOperationException::class)
        private fun processFunc(
            funcAnnotation: Function,
            method: Method,
            result: HashMap<String, Communication>,
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

            result[funcName] = FunctionCommunication { passedArgs ->
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
            }
        }

        private fun processClass(
            namespaceAnnotation: Namespace,
            clazz: Class<*>,
            result: HashMap<String, Communication>,
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
            val communications = process(clazzInst, namespace)

            // put all the communications
            namespace.communications.putAll(communications)

            // finally put it on the results
            result[namespaceName] = namespace
        }
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