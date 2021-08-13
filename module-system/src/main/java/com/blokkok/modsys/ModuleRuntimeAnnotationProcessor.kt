package com.blokkok.modsys

import android.annotation.SuppressLint
import android.os.Build
import com.blokkok.modsys.communication.Communication
import com.blokkok.modsys.communication.FunctionCommunication
import com.blokkok.modsys.modinter.Module
import com.blokkok.modsys.modinter.annotations.Function
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.jvmErasure

/**
 * This class processes annotations of a module and returns a list of communications defined using
 * annotations by the module given
 */
object ModuleRuntimeAnnotationProcessor {

    @SuppressLint("NewApi") // <- false positive
    @Throws(UnsupportedOperationException::class)
    fun process(moduleInst: Module, moduleClass: Class<out Module>): Map<String, Communication> {
        val result = HashMap<String, Communication>()

        // check if this class is made in kotlin (this will be used to check for optional params and
        // parameter names if this class was made in kotlin)
        val isKotlin = moduleClass.getAnnotation(Metadata::class.java) != null

        // check if this is made using kotlin
        if (isKotlin) {
            // alright, since kotlin has optional parameters, name parameters, etc, it's going to be
            // different and easier
            val clazz = moduleClass.kotlin

            // loop through each members of this module class and check if the member is a function
            for (member in clazz.members) {
                if (member !is KFunction) continue
                val funcAnnotation = member.findAnnotation<Function>() ?: continue

                val funcName = if (funcAnnotation.name == "") member.name else funcAnnotation.name

                // map this function's parameters into required and optional hashmaps
                val requiredParams = HashMap<KParameter, Class<*>>()
                val optionalParams = HashMap<KParameter, Class<*>>()

                member.parameters.forEach {
                    val params = if (it.isOptional) optionalParams else requiredParams
                    params[it] = it.type.jvmErasure.java
                }

                // then create a function communication based on the name
                result[funcName] = FunctionCommunication {
                    // do type checks as well as mapping the parameters
                    val args = requiredParams.keys.associateWith { param ->
                        val arg = it[param.name]
                            ?: throw IllegalArgumentException(
                                "Argument ${param.name} must be present"
                            )

                        if (arg.javaClass != param.type.jvmErasure.java)
                            throw ClassCastException(
                                "The type of the argument of \"${param.name}\" " +
                                "(${arg.javaClass.name}) does not have the same type as " +
                                "\"${param.type.jvmErasure.java.name}\""
                            )

                        return@associateWith it[param.name]
                    }

                    // alright, les go call the function!
                    member.call(args)
                }
            }

        } else {
            // loop for each properties in this class and check if the Function or Namespace
            // annotation is present
            for (method in moduleClass.declaredMethods) {
                val funcAnnotation = method.getAnnotation(Function::class.java) ?: continue
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

        return result
    }
}