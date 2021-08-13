package com.blokkok.modsys.modinter.annotations

@Target(AnnotationTarget.FUNCTION)
annotation class Function(
    val name: String = "",
    val paramNames: Array<String> = []
)
