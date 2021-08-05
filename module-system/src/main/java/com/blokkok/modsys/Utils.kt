package com.blokkok.modsys

fun String.isCommunicationName(): Boolean =
    all { it.isLetterOrDigit() || it in listOf('-', '_', '+') }

fun String.capitalizeCompat() = replaceFirstChar { it.uppercase() }

fun List<Char>.generateRandomString(length: Int): String =
    (0..length).map { random() }.joinToString()
