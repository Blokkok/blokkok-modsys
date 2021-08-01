package com.blokkok.modsys.modinter.exception

class AlreadyDefinedException(what: String)
    : RuntimeException("$what is already defined")