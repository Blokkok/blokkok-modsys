package com.blokkok.modsys.modinter.exception

import java.lang.RuntimeException

class NotDefinedException(type: String, name: String)
    : RuntimeException("$type $name doesn't exist")