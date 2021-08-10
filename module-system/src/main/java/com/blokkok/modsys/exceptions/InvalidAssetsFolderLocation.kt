package com.blokkok.modsys.exceptions

class InvalidAssetsFolderLocation(
    moduleName: String,
) : Exception("$moduleName's assets folder is pointing in an invalid location")