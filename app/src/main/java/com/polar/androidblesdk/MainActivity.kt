package com.polar.androidblesdk

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var switchToActivitiesActivity: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        switchToActivitiesActivity = findViewById(R.id.startButton)
        switchToActivitiesActivity.setOnClickListener {
            startActivity(Intent(this@MainActivity, ActivitiesActivity::class.java))
        }
    }
}
