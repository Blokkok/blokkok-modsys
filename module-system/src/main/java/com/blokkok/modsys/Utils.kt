package com.blokkok.modsys

fun String.isCommunicationName(): Boolean =
    all { it.isLetterOrDigit() || it in listOf('-', '_', '+') }