package com.blokkok.modsys.namespace

import com.blokkok.modsys.communication.Communication

/**
 * Stores data about a namespace
 */
data class Namespace(
    val name: String,
    val communications: HashMap<String, Communication> = HashMap(),
    val children: ArrayList<Namespace> = ArrayList(),
    var parent: Namespace? = null,
)