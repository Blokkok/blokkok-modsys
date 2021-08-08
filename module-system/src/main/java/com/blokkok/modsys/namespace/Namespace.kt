package com.blokkok.modsys.namespace

import com.blokkok.modsys.communication.Communication

// TODO: 8/1/21 use hashmap instead of a list for better access time

/**
 * Stores data about a namespace
 */
data class Namespace(
    val name: String,
    val communications: HashMap<String, Communication> = HashMap(),
    val children: ArrayList<Namespace> = ArrayList(),
    var parent: Namespace? = null,
) {
    fun prettyPrint(indentationAmount: Int = 4, indentation: Int = 0, printLine: (String) -> Unit) {
        val indent = " ".repeat(indentation)
        printLine("${indent}N $name")

        communications.entries.forEach {
            printLine("${" ".repeat(indentation + indentationAmount)}C ${it.value.name} \"${it.key}\"")
        }

        printLine("")

        children.forEach {
            it.prettyPrint(indentationAmount = indentationAmount, indentation = indentation + indentationAmount, printLine)
        }
    }
}