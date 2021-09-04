package com.blokkok.modsys.communication

import com.blokkok.modsys.annotations.processor.MethodSpec
import java.lang.reflect.Method

data class ExtensionPointCommunication(
    val spec: Map<MethodSpec, Method>,
    val specClass: Class<*>,
    val implementors: ArrayList<Any> = ArrayList(),
) : Communication() {
    override val name: String = "extension point"
}