package com.blokkok.modsys

import android.annotation.SuppressLint
import android.os.Build
import com.blokkok.modsys.communication.Communication
import com.blokkok.modsys.communication.FunctionCommunication
import com.blokkok.modsys.modinter.Module
import com.blokkok.modsys.modinter.annotations.Function
import java.lang.reflect.Method
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.jvmErasure

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
            KotlinProcessor.processKotlin(moduleClass, moduleInst)
        } else {
            JavaProcessor.processJava(moduleClass, moduleInst)
        }
    }

    /**
     * Runtime annotation processor for kotlin-made modules
     */
    private object KotlinProcessor {
        fun processKotlin(
            moduleClass: Class<out Module>,
            moduleInst: Module,
        ): Map<String, Communication> {
            val result = HashMap<String, Communication>()

            // alright, since kotlin has optional parameters, name parameters, etc, it's going to be
            // different and easier
            val clazz = moduleClass.kotlin

            // loop through each functions of this class
            for (member in clazz.memberFunctions) {
                val funcAnnotation = member.findAnnotation<Function>() ?: continue

                processKotlinFunc(funcAnnotation, member, result, moduleInst)
            }

            return result
        }

        private fun processKotlinFunc(
            funcAnnotation: Function,
            member: KFunction<*>,
            result: HashMap<String, Communication>,
            moduleInst: Module,
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
                        return@associateWith moduleInst

                    val arg = funcCallArgs[param.name]
                        ?: throw IllegalArgumentException(
                            "Argument ${param.name} must be present"
                        )

                    if (arg.javaClass.isAssignableFrom(param.type.jvmErasure.java))
                        throw ClassCastException(
                            "The type of the argument of \"${param.name}\" " +
                                    "(${arg.javaClass.name}) does not have the same type as " +
                                    "\"${param.type.jvmErasure.java.name}\""
                        )

                    return@associateWith funcCallArgs[param.name]
                })

                // add optional params if they're passed
                for (param in optionalParams.keys) {
                    // get the param, if it exists. if it doesn't then just skip
                    val arg: Any = funcCallArgs[param.name] ?: continue

                    // type check!
                    if (arg.javaClass.isAssignableFrom(param.type.jvmErasure.java))
                        throw ClassCastException(
                            "The type of the optional parameter of \"${param.name}\" " +
                                    "(${arg.javaClass.name}) does not have the same type as " +
                                    "\"${param.type.jvmErasure.java.name}\""
                        )

                    // alright, we're good! add it to the args
                    args[param] = arg
                }

                // alright, les go call the function!
                member.callBy(args)
            }
        }
    }

    /**
     * Runtime annotation processor for java-made modules
     */
    private object JavaProcessor {
        @Throws(UnsupportedOperationException::class)
        fun processJava(
            moduleClass: Class<out Module>,
            moduleInst: Module,
        ): Map<String, Communication> {
            val result = HashMap<String, Communication>()

            // loop for each properties in this class and check if the Function or Namespace
            // annotation is present
            for (method in moduleClass.declaredMethods) {
                val funcAnnotation = method.getAnnotation(Function::class.java) ?: continue
                processJavaFunc(funcAnnotation, method, result, moduleInst)
            }

            return result
        }

        @SuppressLint("NewApi") // <- false positive
        @Throws(UnsupportedOperationException::class)
        private fun processJavaFunc(
            funcAnnotation: Function,
            method: Method,
            result: HashMap<String, Communication>,
            moduleInst: Module,
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

            result[funcName] = FunctionCommunication {
                // do type checks as well as mapping the arguments
                val args = params.map { param ->
                    val arg = it[param.key]
                        ?: throw IllegalArgumentException(
                            "Argument ${param.key} must be present"
                        )

                    if (arg.javaClass != param.value)
                        throw ClassCastException(
                            "The type of the argument of \"${param.key}\" " +
                                    "(${arg.javaClass.name}) does not have the same type as " +
                                    "\"${param.value.name}\""
                        )

                    return@map param.value
                }.toTypedArray()

                // alright, les go invoke the function!
                method.invoke(moduleInst, *args)
            }
        }
    }
}