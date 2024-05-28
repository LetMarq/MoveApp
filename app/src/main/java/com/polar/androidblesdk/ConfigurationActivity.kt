package com.polar.androidblesdk

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class ConfigurationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuration)

        val editTextDeviceId = findViewById<EditText>(R.id.editTextDeviceId)
        val buttonSave = findViewById<Button>(R.id.buttonSave)

        val sharedPref = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val currentDeviceId = sharedPref.getString("deviceId", "")

        // Exibindo o valor atual de deviceId no campo de texto
        editTextDeviceId.setText(currentDeviceId)

        buttonSave.setOnClickListener {
            val newDeviceId = editTextDeviceId.text.toString()

            with(sharedPref.edit()) {
                putString("deviceId", newDeviceId)
                apply()
            }

            Toast.makeText(this, "Device ID saved successfully", Toast.LENGTH_SHORT).show()
        }
    }
}