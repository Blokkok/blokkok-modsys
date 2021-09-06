package com.blokkok.modsys.modinter.annotations

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class ExtensionPoint(
    val name: String = ""
)