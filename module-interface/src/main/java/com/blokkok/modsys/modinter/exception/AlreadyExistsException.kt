package com.blokkok.modsys.modinter.exception

class AlreadyExistsException(what: String)
    : RuntimeException("$what already exists")