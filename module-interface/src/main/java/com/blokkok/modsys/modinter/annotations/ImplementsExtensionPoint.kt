package com.blokkok.modsys.modinter.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ImplementsExtensionPoint(
    val namespace: String = "/",
    val name: String,
)