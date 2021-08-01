package com.blokkok.modsys.namespace

import com.blokkok.modsys.communication.Communication

// TODO: 8/1/21 use hashmap instead of a list for better access time

/**
 * Stores data about a namespace
 */
data class Namespace(
    val name: String,
    val communications: HashMap<String, Communication> = HashMap(),
    val children: ArrayList<Namespace> = ArrayList(),
    var parent: Namespace? = null,
)