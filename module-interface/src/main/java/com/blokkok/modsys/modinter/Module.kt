package com.blokkok.modsys.modinter

import com.blokkok.modsys.ModuleLoader

abstract class Module(bridge: ModuleLoader.ModuleBridge) {
    abstract fun onLoad()
    abstract fun onExit()
}