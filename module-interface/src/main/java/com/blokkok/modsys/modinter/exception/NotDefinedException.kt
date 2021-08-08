package com.blokkok.modsys.modinter.exception

class NotDefinedException(type: String, name: String)
    : RuntimeException("$type \"$name\" doesn't exist")