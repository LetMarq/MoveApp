package com.polar.androidblesdk

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var switchToActivitiesActivity: ImageButton
    private lateinit var switchToConfigActivity: ImageButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sharedPref = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        if (!sharedPref.contains("deviceId")) {
            with(sharedPref.edit()) {
                putString("deviceId", "C3E38426")
                apply()
            }
        }

        switchToActivitiesActivity = findViewById(R.id.startButton)
        switchToActivitiesActivity.setOnClickListener {
            startActivity(Intent(this@MainActivity, ActivitiesActivity::class.java))
        }


        switchToConfigActivity = findViewById(R.id.configButton)
        switchToConfigActivity.setOnClickListener {
            startActivity(Intent(this@MainActivity, ConfigurationActivity::class.java))
        }
    }
}
