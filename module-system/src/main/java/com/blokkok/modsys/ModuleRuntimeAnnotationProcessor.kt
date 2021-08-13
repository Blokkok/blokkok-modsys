package com.blokkok.modsys

import android.annotation.SuppressLint
import android.os.Build
import com.blokkok.modsys.communication.Communication
import com.blokkok.modsys.communication.FunctionCommunication
import com.blokkok.modsys.modinter.Module
import com.blokkok.modsys.modinter.annotations.Function

/**
 * This class processes annotations of a module and returns a list of communications defined using
 * annotations by the module given
 */
object ModuleRuntimeAnnotationProcessor {

    // TODO: 8/13/21 Make so that java users can map their function parameters

    @SuppressLint("NewApi") // <- false positive
    @Throws(UnsupportedOperationException::class)
    fun process(moduleInst: Module, moduleClass: Class<out Module>): Map<String, Communication> {
        val result = HashMap<String, Communication>()

        // check if this class is made in kotlin (this will be used to check for optional params and
        // parameter names if this class was made in kotlin)
        val isKotlin = moduleClass.getAnnotation(Metadata::class.java) != null

        // loop for each properties in this class and check if the Function or Namespace
        // annotation is present
        for (method in moduleClass.declaredMethods) {
            val funcAnnotation = method.getAnnotation(Function::class.java) ?: continue
            val funcName = if (funcAnnotation.name == "") method.name else funcAnnotation.name

            // since java doesn't support parameter names like kotlin does, we will need to map
            // the parameter names into its parameter index and its type

            // sadly, this can only be done on API level 26+
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) throw UnsupportedOperationException()

            val params = method.parameters.associate { Pair(it.name, it.type) }

            result[funcName] = FunctionCommunication {
                // do type checks as well as mapping the arguments
                val args = params.map { param ->
                    val arg = it[param.key]
                        ?: throw IllegalArgumentException("Argument ${param.key} must be present")

                    if (arg.javaClass != param.value)
                        throw ClassCastException(
                            "The type of the argument of \"${param.key}\" (${arg.javaClass.name})" +
                            " does not have the same type as \"${param.value.name}\""
                        )

                    return@map param.value
                }.toTypedArray()

                // alright, les go invoke the function!
                method.invoke(moduleInst, *args)
            }
        }

        return result
    }
}