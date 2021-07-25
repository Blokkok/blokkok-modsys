package com.blokkok.modsys.example

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.iyxan23.ems.databinding.ActivityMainBinding
import com.blokkok.modsys.modinter.communication.Broadcaster
import com.blokkok.modsys.ModuleManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // This broadcaster is used when this activity has started
    private lateinit var launchBroadcaster: Broadcaster

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        ModuleManager.initialize(this)

        binding.openManager.setOnClickListener {
            startActivity(
                Intent(this@MainActivity, ModuleManagerActivity::class.java)
            )
        }

        ModuleManager.registerCommunications {
            launchBroadcaster = createBroadcaster("main_activity_on_launched")

            registerFunction("add_text") {
                Log.d("MainActivity", "onCreate: add_text got called!")

                it.forEach { elem ->
                    // type checking
                    if (elem !is String) return@registerFunction null

                    // add a textview to the container
                    binding.randomHolder.addView(TextView(this@MainActivity).apply {
                        text = elem
                    })
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        launchBroadcaster.broadcast()
    }
}