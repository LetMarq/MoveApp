package com.polar.androidblesdk

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class FrequencyActivity : AppCompatActivity() {
    private lateinit var selectMetronomo: ImageButton
    private lateinit var selectHeartRate: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_frequency)

        selectMetronomo = findViewById(R.id.metronomoButton)
        selectHeartRate = findViewById(R.id.hearRateButton)

        selectMetronomo.setOnClickListener {
            startActivity(Intent(this@FrequencyActivity, MetronomActivity::class.java))
        }

        selectHeartRate.setOnClickListener {
            startActivity(Intent(this@FrequencyActivity, HeartRateActivity::class.java))
        }
    }
}
