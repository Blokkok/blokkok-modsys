package com.blokkok.modsys.modinter.exception

class FlagAlreadyClaimedException(flagName: String)
    : Exception("Flag \"$flagName\" is already claimed")