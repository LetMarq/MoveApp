package com.polar.androidblesdk

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class ActivitiesActivity : AppCompatActivity() {
    private lateinit var selectNatacao: ImageButton
    private lateinit var selectRunning: ImageButton
    private lateinit var selectCycling: ImageButton
    private lateinit var switchToConfigActivity: ImageButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_activities)

        selectNatacao = findViewById(R.id.natacaoButton)
        selectRunning = findViewById(R.id.runButton)
        selectCycling = findViewById(R.id.cyclingButton)
        switchToConfigActivity = findViewById(R.id.configButton)


        selectNatacao.setOnClickListener {
            startActivity(Intent(this@ActivitiesActivity, FrequencyActivity::class.java))
        }

        selectRunning.setOnClickListener {
            startActivity(Intent(this@ActivitiesActivity, FrequencyActivity::class.java))
        }

        selectCycling.setOnClickListener {
            startActivity(Intent(this@ActivitiesActivity, FrequencyActivity::class.java))
        }

        switchToConfigActivity.setOnClickListener {
            startActivity(Intent(this@ActivitiesActivity, ConfigurationActivity::class.java))
        }
    }
}
