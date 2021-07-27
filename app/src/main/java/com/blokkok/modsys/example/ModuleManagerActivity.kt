package com.blokkok.modsys.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.blokkok.modsys.ModuleManager
import com.blokkok.modsys.example.databinding.ActivityModuleManagerBinding
import java.util.zip.ZipInputStream

class ModuleManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityModuleManagerBinding
    private val adapter = ModulesRecyclerViewAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModuleManagerBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.loadAllButton.setOnClickListener { ModuleManager.loadModules(this) }
        binding.unloadAllButton.setOnClickListener { ModuleManager.unloadModules() }

        val importModule = registerForActivityResult(ActivityResultContracts.OpenDocument()) {
            if (it == null) return@registerForActivityResult

            val descriptor = contentResolver.openAssetFileDescriptor(it, "r")!!
            val inputStream = descriptor.createInputStream()

            ModuleManager.importModule(ZipInputStream(inputStream))

            Toast.makeText(this, "Module has successfully imported", Toast.LENGTH_SHORT).show()

            listModules()
        }

        binding.importModuleFab.setOnClickListener { importModule.launch(arrayOf("*/*")) }
        binding.modulesRv.layoutManager = LinearLayoutManager(this)
        binding.modulesRv.adapter = adapter

        listModules()
    }

    private fun listModules() {
        val modules = ModuleManager.listModules().values

        adapter.modules = modules.toList()
        adapter.notifyDataSetChanged()
    }
}