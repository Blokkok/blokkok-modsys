package com.blokkok.modsys

fun String.isAlphanumeric(): Boolean = all { it.isLetterOrDigit() }