package com.example.cadence_metronome.presentation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class CadenceService : Service() {
    private var serviceJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private lateinit var vibrator: Vibrator
    private var toneGenerator: ToneGenerator? = null

    override fun onCreate() {
        super.onCreate()
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "START") {
            val bpm = intent.getIntExtra("BPM", 180)
            val useVibration = intent.getBooleanExtra("VIBRATION", true)
            val useSound = intent.getBooleanExtra("SOUND", false)
            startForegroundService(bpm)
            startMetronome(bpm, useVibration, useSound)
        } else if (action == "STOP") {
            stopMetronome()
            stopSelf()
        }
        return START_STICKY
    }

    private fun startForegroundService(bpm: Int) {
        val notification = NotificationCompat.Builder(this, "cadence_channel")
            .setContentTitle("BPM 180 (Running)")
            .setContentText("Current Cadence: $bpm BPM")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()
        startForeground(1, notification)
    }

    private fun startMetronome(bpm: Int, useVibration: Boolean, useSound: Boolean) {
        serviceJob?.cancel()
        serviceJob = serviceScope.launch {
            val interval = (60000 / bpm).toLong()
            while (isActive) {
                if (useVibration) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                }
                if (useSound) {
                    toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 50)
                }
                delay(interval)
            }
        }
    }

    private fun stopMetronome() {
        serviceJob?.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "cadence_channel",
                "Cadence Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        toneGenerator?.release()
    }
}
