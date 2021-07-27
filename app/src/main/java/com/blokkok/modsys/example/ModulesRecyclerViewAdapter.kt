package com.blokkok.modsys.example

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.blokkok.modsys.ModuleManager
import com.blokkok.modsys.models.ModuleMetadata

class ModulesRecyclerViewAdapter : RecyclerView.Adapter<ModulesRecyclerViewAdapter.ViewHolder>() {

    var modules: List<ModuleMetadata> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            LayoutInflater
                .from(parent.context)
                .inflate(R.layout.module_item, parent, false)
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentModule = modules[position]

        holder.name.text = currentModule.name
        holder.description.text = currentModule.description
        holder.enableSwitch.isChecked = currentModule.enabled

        holder.enableSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked) ModuleManager.enableModule (currentModule.id)
            else         ModuleManager.disableModule(currentModule.id)
        }
    }

    override fun getItemCount(): Int = modules.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.module_name)
        val description: TextView = itemView.findViewById(R.id.module_description)
        val enableSwitch: SwitchMaterial = itemView.findViewById(R.id.enable_switch)
    }
}