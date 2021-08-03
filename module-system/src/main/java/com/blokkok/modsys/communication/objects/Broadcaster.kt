package com.blokkok.modsys.communication.objects

abstract class Broadcaster {
    abstract fun broadcast(vararg args: Any?)
}