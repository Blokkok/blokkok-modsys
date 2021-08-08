package com.blokkok.modsys

import com.blokkok.modsys.models.ModuleMetadata

/**
 * This class is used to order modules and its dependencies in a correct way (dependencies goes
 * first, then the module, etc)
 */
class ModuleDependencyResolver(
    private val modules: List<ModuleMetadata>
) {
    private val modulesMapped: Map<String, ModuleMetadata> = modules.associateBy { "${it.name}:${it.version}" }
    private val blacklistedNodes = ArrayList<String>()

    // since dependency trees can be separated from each other, we need a list to store them
    private val tree = ArrayList<ModuleNode>()

    @Throws(ModuleDependencyNotFoundException::class)
    fun orderModules(): List<ModuleMetadata> {
        // parse the modules into a tree of dependencies
        parseToTree()

        // and then traverse the tree to get the lowest nodes to be loaded, then up and up and up
        return ArrayList<ModuleMetadata>().apply {
            tree.forEach { node ->
                addAll(traverseNodeChildren(node))
            }
        }
    }

    private fun traverseNodeChildren(node: ModuleNode): List<ModuleMetadata> {
        if (node.children.isEmpty())
            // this node has no children
            return listOf(modulesMapped[node.name]!!)

        return ArrayList<ModuleMetadata>().apply {
            node.children.forEach { childNode ->
                addAll(traverseNodeChildren(childNode))
            }
        }
    }

    private fun parseToTree() {
        tree.apply {
            modules.forEach {
                if (it.name in blacklistedNodes) return@forEach

                add(resolveChildren(it))
            }
        }
    }

    @Throws(ModuleDependencyNotFoundException::class)
    private fun resolveChildren(rawMeta: ModuleMetadata, parent: ModuleNode? = null): ModuleNode {
        val thisNode = ModuleNode(rawMeta.name, parent = parent)

        val nodes: List<ModuleNode> = rawMeta.dependencies.mapNotNull { dependency ->
            if (dependency in blacklistedNodes) {
                // this dependency is already added to the tree
                // since this is a dependency tree, we don't want the same module being loaded multiple times
                // and also, because this dependency will be loaded before this module is loaded,
                // we don't need to add this
                return@mapNotNull null
            }

            try {
                val module = modulesMapped[dependency]
                    ?:
                    // If there is no module with this name
                    // Check if this module is already loaded before
                    if (ModuleLoader
                            .listLoadedModulesMetadata()
                            .find { "${it.name}:${it.version}" == dependency } != null
                    ) {
                        // alright, we can skip this then
                        return@mapNotNull null
                    } else {
                        // no, it's not loaded nor is it being loaded
                        throw ModuleDependencyNotFoundException(rawMeta.name, dependency)
                    }

                return@mapNotNull resolveChildren(
                    module,
                    thisNode
                )

            } finally {
                blacklistedNodes.add(dependency)
            }
        }

        thisNode.children.addAll(nodes)

        return thisNode
    }

    data class ModuleNode(
        val name: String,
        val children: ArrayList<ModuleNode> = ArrayList(),
        val alreadyPresentChildren: ArrayList<ModuleNode> = ArrayList(),
        var parent: ModuleNode? = null,
    )

    inner class ModuleDependencyNotFoundException(
        moduleNotFoundName: String,
        moduleTryingToFindName: String,
    ) : Exception("Module $moduleTryingToFindName is dependant of $moduleNotFoundName but can't find it")
}