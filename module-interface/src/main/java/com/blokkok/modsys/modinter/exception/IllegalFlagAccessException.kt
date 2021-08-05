package com.blokkok.modsys.modinter.exception

class IllegalFlagAccessException(flagName: String)
    : RuntimeException("This module has not claimed the flag \"$flagName\"")