package com.blokkok.modsys.exceptions

class SameIDException(
    id: String,
    otherModuleName: String
) : Exception("This module has the same ID as $otherModuleName ($id)")