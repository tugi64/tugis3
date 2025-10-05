package com.example.tugis3.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.tugis3.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_project -> {
                    Toast.makeText(this, "Project menüsü seçildi", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_device -> {
                    Toast.makeText(this, "Device menüsü seçildi", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_survey -> {
                    Toast.makeText(this, "Survey menüsü seçildi", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_tools -> {
                    Toast.makeText(this, "Tools menüsü seçildi", Toast.LENGTH_SHORT).show()
                    true
                }
                // CAD menüsü zaten hazır, dokunulmuyor
                else -> false
            }
        }
    }
}
