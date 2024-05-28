package com.polar.androidblesdk

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.util.Pair
import com.google.android.material.snackbar.Snackbar
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.PolarH10OfflineExerciseApi
import com.polar.sdk.api.errors.PolarInvalidArgument
import com.polar.sdk.api.model.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import java.util.*
import android.media.AudioManager
import android.media.ToneGenerator
import android.widget.EditText
import android.widget.ImageButton
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class HeartRateActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val API_LOGGER_TAG = "API LOGGER"
        private const val PERMISSION_REQUEST_CODE = 1
    }

    private var deviceId = "C3E38426"
    private var toneGenerator: ToneGenerator? = null
    private var metronomeTimer: Timer? = null

    private var frequencyChangeTimer: Timer? = null

    private val api: PolarBleApi by lazy {
        PolarBleApiDefaultImpl.defaultImplementation(
            applicationContext,
            setOf(
                PolarBleApi.PolarBleSdkFeature.FEATURE_HR,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_SDK_MODE,
                PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_H10_EXERCISE_RECORDING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_OFFLINE_RECORDING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_DEVICE_TIME_SETUP,
                PolarBleApi.PolarBleSdkFeature.FEATURE_DEVICE_INFO,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_LED_ANIMATION
            )
        )
    }
    private var scanDisposable: Disposable? = null
    private var autoConnectDisposable: Disposable? = null
    private var hrDisposable: Disposable? = null
    private var ecgDisposable: Disposable? = null
    private var accDisposable: Disposable? = null
    private var gyrDisposable: Disposable? = null
    private var magDisposable: Disposable? = null
    private var ppgDisposable: Disposable? = null
    private var isMetronomeActive = false

    private var deviceConnected = false
    private var bluetoothEnabled = false

    private lateinit var broadcastButton: ImageButton
    private lateinit var startButton: ImageButton
    private lateinit var switchToConfigActivity: ImageButton
    private lateinit var hrDisplayTextView: TextView
    private lateinit var maxHrEditText: EditText
    private lateinit var minHrEditText: EditText
    private lateinit var startButtonText: TextView
    private lateinit var initialFrequencyEditText: EditText
    private lateinit var initialTimeEditText: EditText

    private var currentInterval: Long? = null
    private var currentFrequency: Int = 60  // Valor padrão
    @Volatile private var latestHR: Int = 0  // Latest heart rate received
    private var scheduledExecutorService: ScheduledExecutorService? = null





    private val entryCache: MutableMap<String, MutableList<PolarOfflineRecordingEntry>> = mutableMapOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_heart_rate)
        Log.d(TAG, "version: " + PolarBleApiDefaultImpl.versionInfo())
        broadcastButton = findViewById(R.id.broadcast_button)
        startButton = findViewById(R.id.start_button)
        hrDisplayTextView = findViewById(R.id.hr_display)
        maxHrEditText = findViewById(R.id.edit_text_max_hr)
        minHrEditText = findViewById(R.id.edit_text_min_hr)
        initialFrequencyEditText = findViewById(R.id.frequency_display_text)
        initialTimeEditText = findViewById(R.id.time_display_text)
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

        val sharedPref = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        deviceId = sharedPref.getString("deviceId", deviceId) ?: deviceId

        // Registrando uma mensagem de log para verificar o valor recuperado
        showToast("Device ID: $deviceId")

        startButtonText = findViewById(R.id.text_stop_begin_button)  // Initialize your TextView

//        frequencyDisplayText = findViewById(R.id.frequency_display_text)
//        timeDisplayText = findViewById(R.id.time_display_text)
        toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100) // Adjust the volume level (100 is maximum)

        switchToConfigActivity = findViewById(R.id.configButton)
        switchToConfigActivity.setOnClickListener {
            startActivity(Intent(this@HeartRateActivity, ConfigurationActivity::class.java))
        }

        api.setPolarFilter(false)

        val enableSdkLogs = false
        if(enableSdkLogs) {
            api.setApiLogger { s: String -> Log.d(API_LOGGER_TAG, s) }
        }

        api.setApiCallback(object : PolarBleApiCallback() {
            override fun blePowerStateChanged(powered: Boolean) {
                Log.d(TAG, "BLE power: $powered")
                bluetoothEnabled = powered
                if (powered) {
                    enableAllButtons()
                    showToast("Phone Bluetooth on")
                } else {
                    disableAllButtons()
                    showToast("Phone Bluetooth off")
                }
            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "CONNECTED: ${polarDeviceInfo.deviceId}")
                deviceId = polarDeviceInfo.deviceId
                deviceConnected = true
                val buttonText = getString(R.string.disconnect_from_device, deviceId)
//                toggleButtonDown(connectButton, buttonText)
            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "CONNECTING: ${polarDeviceInfo.deviceId}")
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "DISCONNECTED: ${polarDeviceInfo.deviceId}")
                deviceConnected = false
                val buttonText = getString(R.string.connect_to_device, deviceId)
//                toggleButtonUp(connectButton, buttonText)
//                toggleButtonUp(toggleSdkModeButton, R.string.enable_sdk_mode)
            }


            private var toneTimer: Timer? = null

            override fun hrNotificationReceived(identifier: String, data: PolarHrData.PolarHrSample) {
                val hr = data.hr
                synchronized(this) {
                    latestHR = hr  // Update the shared heart rate variable
                }

                runOnUiThread {
                    hrDisplayTextView.text = "Frequência cardíaca: $hr"
                }
            }


            override fun disInformationReceived(identifier: String, uuid: UUID, value: String) {
                Log.d(TAG, "DIS INFO uuid: $uuid value: $value")
            }

            override fun batteryLevelReceived(identifier: String, level: Int) {
                Log.d(TAG, "BATTERY LEVEL: $level")
            }

        })

        broadcastButton.setOnClickListener {
            connectToDevice { isConnected ->
                if (isConnected) {
                    autoConnectToDevice { isAutoConnected ->
                        if (isAutoConnected) {
                            scanForDevices { isScanComplete ->
                                if (isScanComplete) {
                                    startHrStreaming()
                                }
                            }
                        }
                    }
                }
            }
        }

        startButton.setOnClickListener {
            isMetronomeActive = !isMetronomeActive

            if (isMetronomeActive) {
                startButtonText.text = "Parar"
                currentFrequency = initialFrequencyEditText.text.toString().toIntOrNull() ?: 60
                showToast("Metronome monitoring activated.")
                scheduleFrequencyAdjustment()
            } else {
                stopMetronome()
                startButtonText.text = "Iniciar"
                showToast("Metronome monitoring deactivated.")
            }
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT), PERMISSION_REQUEST_CODE)
            } else {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
            }
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (index in 0..grantResults.lastIndex) {
                if (grantResults[index] == PackageManager.PERMISSION_DENIED) {
                    disableAllButtons()
                    Log.w(TAG, "No sufficient permissions")
                    showToast("No sufficient permissions")
                    return
                }
            }
            Log.d(TAG, "Needed permissions are granted")
            enableAllButtons()
        }
    }

    public override fun onPause() {
        super.onPause()
    }

    public override fun onResume() {
        super.onResume()
        api.foregroundEntered()
    }

    public override fun onDestroy() {
        super.onDestroy()
        api.shutDown()
        toneGenerator?.release()
    }

    private fun showToast(message: String) {
        val toast = Toast.makeText(applicationContext, message, Toast.LENGTH_LONG)
        toast.show()
    }

    private fun scheduleFrequencyAdjustment() {
        scheduledExecutorService?.scheduleAtFixedRate({
            val maxHr = maxHrEditText.text.toString().toIntOrNull()
            val minHr = minHrEditText.text.toString().toIntOrNull()
            Log.d(TAG, "HR: ${latestHR.toString()} | FR: ${currentFrequency.toString()}")

            if (maxHr != null && minHr != null) {
                synchronized(this) {
                    if (latestHR < maxHr) {
                        currentFrequency += 2
                    } else if (latestHR > minHr) {
                        currentFrequency -= 2
                    }
                }
                startMetronomeWithFrequency(currentFrequency)
            }
        }, 0, 5, TimeUnit.SECONDS)
    }

    private fun disableAllButtons() {
        broadcastButton.isEnabled = false
    }

    private fun enableAllButtons() {
        broadcastButton.isEnabled = true
    }


    private fun connectToDevice(completion: (Boolean) -> Unit) {
        if (!deviceConnected) {
            try {
                api.connectToDevice(deviceId)
                completion(true) // Simulate a successful connection
            } catch (e: PolarInvalidArgument) {
                Log.e(TAG, "Failed to connect. Reason: ${e.message}")
                completion(false)
            }
        } else {
            Log.d(TAG, "Device already connected.")
            completion(true)
        }
    }

    private fun autoConnectToDevice(completion: (Boolean) -> Unit) {
        if (autoConnectDisposable == null || autoConnectDisposable?.isDisposed == true) {
            autoConnectDisposable = api.autoConnectToDevice(-60, "180D", null)
                .subscribe(
                    {
                        Log.d(TAG, "Auto-connect search complete")
                        completion(true)
                    },
                    { throwable ->
                        Log.e(TAG, "Auto-connect failed: ${throwable.message}")
                        completion(false)
                    }
                )
        } else {
            Log.d(TAG, "Auto-connect already in progress.")
            completion(false)
        }
    }

    private fun scanForDevices(completion: (Boolean) -> Unit) {
        val isDisposed = scanDisposable?.isDisposed ?: true
        if (isDisposed) {
            scanDisposable = api.searchForDevice()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { polarDeviceInfo: PolarDeviceInfo ->
                        Log.d(TAG, "Polar device found: id=${polarDeviceInfo.deviceId}, address=${polarDeviceInfo.address}")
                        completion(true)
                    },
                    { error ->
                        Log.e(TAG, "Device scan failed. Reason: ${error.message}")
                        completion(false)
                    },
                    {
                        Log.d(TAG, "Device scan complete")
                        completion(true)
                    }
                )
        } else {
            Log.d(TAG, "Scanning already in progress.")
            completion(false)
        }
    }

    private fun startHrStreaming() {
        val isDisposed = hrDisposable?.isDisposed ?: true
        if (isDisposed) {
            hrDisposable = api.startHrStreaming(deviceId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { hrData: PolarHrData ->
                        Log.d(TAG, "HR Data received: bpm=${hrData.samples.map { it.hr }}")
                    },
                    { error ->
                        Log.e(TAG, "HR streaming failed. Reason: ${error.message}")
                    },
                    {
                        Log.d(TAG, "HR streaming complete")
                    }
                )
        } else {
            Log.d(TAG, "HR streaming already in progress.")
        }
    }
    private fun startMetronomeWithFrequency(frequency: Int) {
        val interval = 60000L / frequency
        metronomeTimer?.cancel()
        metronomeTimer = Timer().apply {
            scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 100)
                }
            }, 0, interval)
        }
    }

    private fun stopMetronome() {
        // This will stop all currently executing tasks and prevent scheduled tasks from starting.
        scheduledExecutorService?.shutdownNow()
        try {
            // Ensure termination of all tasks.
            if (!scheduledExecutorService?.awaitTermination(1, TimeUnit.SECONDS)!!) {
                scheduledExecutorService?.shutdownNow()
            }
        } catch (ie: InterruptedException) {
            // Re-cancel if current thread also interrupted
            scheduledExecutorService?.shutdownNow()
            Thread.currentThread().interrupt()  // preserve interrupt status
        }

        // Nullify the executor service if you plan to reinitialize it later
        scheduledExecutorService = null

        // Stop the tone generator if used for playing sound
        toneGenerator?.stopTone()
        toneGenerator?.release()
        toneGenerator = null
    }


}