package com.blokkok.modsys

object ModuleFlagsManager {
    private val flags = HashMap<String, ArrayList<ModuleContainer>>()
    private val flagOwners = HashMap<String, String>()

    fun putFlag(name: String, module: ModuleContainer) =
        (flags[name] ?: ArrayList<ModuleContainer>().also { flags[name] = it }).add(module)

    fun claimFlag(name: String): String? {
        // check if the flag is already claimed
        if (name in flagOwners) return null

        // initialize the arraylist if it's null
        if (flags[name] == null) flags[name] = ArrayList()

        // generates a random string used to identify the owner of the flag
        val random = (('a'..'z') + ('A'..'Z') + ('0'..'9')).generateRandomString(16)
        flagOwners[name] = random
        return random
    }

    fun getModulesWithFlag(flagName: String, claimId: String): List<ModuleContainer>? {
        return if (flagOwners[flagName] == claimId) {
            /** !! because it's guaranteed to be initialized on [claimFlag] */
            flags[flagName]!!.toList()
        } else null
    }
}