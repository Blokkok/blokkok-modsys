package com.blokkok.modsys.annotations.processor

import java.lang.reflect.Method
import java.lang.reflect.Type

data class MethodSpec(
    val name: String,
    val returnType: Type,
    val parameters: List<Type>,
) {
    companion object {
        fun fromMethod(method: Method): MethodSpec =
            MethodSpec(
                method.name,
                method.returnType,
                method.parameterTypes.toList()
            )
    }

    override fun toString(): String {
        return  "Method name: \"$name\", " +
                "Return type: $returnType, " +
                "Parameters: ${parameters.joinToString(",")}"
    }
}