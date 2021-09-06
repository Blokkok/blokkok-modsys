package com.blokkok.modsys.communication.namespace

import com.blokkok.modsys.communication.Communication

/**
 * Stores data about a namespace
 */
class Namespace(
    val namespaceName: String,
    val communications: HashMap<String, Communication> = HashMap(),
    var parent: Namespace? = null,
): Communication() {
    override val name: String get() = "namespace"

    fun prettyPrint(indentationAmount: Int = 4, indentation: Int = 0, printLine: (String) -> Unit) {
        val indent = " ".repeat(indentation)
        printLine("${indent}N $namespaceName")

        communications.entries.forEach {
            if (it.value is Namespace) {
                (it.value as Namespace)
                    .prettyPrint(
                        indentationAmount = indentationAmount,
                        indentation = indentation + indentationAmount,
                        printLine
                    )

            } else {
                printLine("${" ".repeat(indentation + indentationAmount)}C ${it.value.name} \"${it.key}\"")
            }
        }

        printLine("")
    }
}