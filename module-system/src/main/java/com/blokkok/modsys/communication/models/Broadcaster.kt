package com.blokkok.modsys.communication.models

abstract class Broadcaster {
    abstract fun broadcast(vararg args: Any?)
}