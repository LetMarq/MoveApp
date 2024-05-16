package com.polar.androidblesdk

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.widget.Button
import android.widget.Chronometer
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class MetronomActivity : AppCompatActivity() {

    private lateinit var mStartButton: Button
    private lateinit var mStopButton: Button
    private lateinit var mChrono: Chronometer
    private var lastPause: Long = 0
    private val metronomeHandler = Handler(Looper.getMainLooper())
    private var toneGenerator: ToneGenerator? = null
    private val bpmEditTexts = ArrayList<EditText>()
    private val timeEditTexts = ArrayList<EditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_metronom)

        mStartButton = findViewById(R.id.buttonStartMetronomo)
        mStopButton = findViewById(R.id.buttonStopMetronomo)
        mChrono = findViewById(R.id.chronometer)

        for (i in 1..10) {
            val bpmEditTextId = resources.getIdentifier("bpm$i", "id", packageName)
            val timeEditTextId = resources.getIdentifier("time$i", "id", packageName)

            val bpmEditText = findViewById<EditText>(bpmEditTextId)
            val timeEditText = findViewById<EditText>(timeEditTextId)

            if (bpmEditText != null && timeEditText != null) {
                bpmEditTexts.add(bpmEditText)
                timeEditTexts.add(timeEditText)
            }
        }

        mStartButton.setOnClickListener {
            if (lastPause != 0L) {
                mChrono.base = mChrono.base + SystemClock.elapsedRealtime() - lastPause
            } else {
                mChrono.base = SystemClock.elapsedRealtime()
            }

            startMetronome()
            mChrono.start()
            disableAllEditTexts()
            mStartButton.isEnabled = false
            mStopButton.isEnabled = true
        }

        mStopButton.setOnClickListener {
            mChrono.stop()
            mChrono.base = SystemClock.elapsedRealtime()
            lastPause = 0
            stopMetronome()
            enableAllEditTexts()
            mStartButton.isEnabled = true
            mStopButton.isEnabled = false
        }

        // Initialize ToneGenerator for beep sound
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        } catch (e: Exception) {
            Log.e("Metronome", "ToneGenerator is not supported on this device")
        }
    }

    private fun disableAllEditTexts() {
        for (editText in bpmEditTexts) {
            editText.isEnabled = false
        }
        for (editText in timeEditTexts) {
            editText.isEnabled = false
        }
    }

    private fun enableAllEditTexts() {
        for (editText in bpmEditTexts) {
            editText.isEnabled = true
        }
        for (editText in timeEditTexts) {
            editText.isEnabled = true
        }
    }

    private fun startMetronome() {
        var currentIndex = 0 // Keep track of the current BPM index
        var startTime = SystemClock.elapsedRealtime()

        metronomeHandler.post(object : Runnable {
            override fun run() {
                // Check if the current index is valid
                if (currentIndex < 0 || currentIndex >= bpmEditTexts.size) {
                    // Stop metronome and chronometer if the index is out of bounds
                    Log.e("Metronome", "Invalid index: $currentIndex")
                    stopMetronome()
                    stopChronometer()
                    return
                }

                // Get current BPM and time
                val bpmString = bpmEditTexts[currentIndex].text.toString()
                val timeString = timeEditTexts[currentIndex].text.toString()

                // Default values in case of empty or non-numeric input
                var bpm = 0
                var time = 0

                // Parse BPM and time only if they are not empty
                if (bpmString.isNotEmpty() && timeString.isNotEmpty()) {
                    try {
                        bpm = bpmString.toInt()
                        time = timeString.toInt()
                    } catch (e: NumberFormatException) {
                        // Handle the case where parsing fails (e.g., non-numeric input)
                        Log.e("Metronome", "Error parsing BPM or time: ${e.message}")
                    }
                }

                // Play metronome sound
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP)

                // Check if the elapsed time exceeds the specified duration
                if (SystemClock.elapsedRealtime() - startTime >= (time * 1000) - 700) {
                    // Move to the next BPM
                    currentIndex++

                    // If there are more BPM values, reset elapsed time for the next BPM
                    if (currentIndex < bpmEditTexts.size) {
                        startTime = SystemClock.elapsedRealtime()
                    } else {
                        stopMetronome()
                        stopChronometer()
                        toneGenerator?.release() // Release the ToneGenerator when done
                        return
                    }
                }

                // Schedule the next metronome click based on the current BPM
                val delay = (60 * 1000) / if (bpm > 0) bpm else 1 // Ensure non-zero value for BPM
                metronomeHandler.postDelayed(this, delay.toLong())
            }
        })
    }

    private fun stopChronometer() {
        mChrono.stop()
        mChrono.base = SystemClock.elapsedRealtime()
        lastPause = 0
    }

    private fun stopMetronome() {
        metronomeHandler.removeCallbacksAndMessages(null)
        stopChronometer()
    }

    override fun onDestroy() {
        super.onDestroy()
        toneGenerator?.release()
        toneGenerator = null
        stopMetronome()
    }
}
