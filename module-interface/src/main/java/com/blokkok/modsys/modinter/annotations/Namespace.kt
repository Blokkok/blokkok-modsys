package com.blokkok.modsys.modinter.annotations

@Target(AnnotationTarget.CLASS)
annotation class Namespace(
    val name: String = "",
)
