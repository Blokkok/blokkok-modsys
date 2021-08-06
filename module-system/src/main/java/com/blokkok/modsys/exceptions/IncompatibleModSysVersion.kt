package com.blokkok.modsys.exceptions

class IncompatibleModSysVersion(
    moduleName: String,
    moduleVersion: String,
    libraryVersion: String,
) : Exception("Module $moduleName (uses library $moduleVersion) is not compatible with the current library version $libraryVersion")