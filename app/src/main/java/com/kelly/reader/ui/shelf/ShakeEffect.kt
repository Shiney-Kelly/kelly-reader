/*
 * Listens to the accelerometer for a long shake while a screen is in front.
 */
package com.kelly.reader.ui.shelf

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect

private const val NANOS_PER_MS = 1_000_000L

/**
 * Calls [onShake] once the phone has been shaken for a few seconds. The accelerometer is only
 * listened to while the screen is resumed, and a phone without one never calls it.
 */
@Composable
fun ShakeEffect(onShake: () -> Unit) {
    val context = LocalContext.current
    val sensorManager = remember(context) { context.getSystemService(SensorManager::class.java) }
    val currentOnShake by rememberUpdatedState(onShake)
    LifecycleResumeEffect(sensorManager) {
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val detector = ShakeDetector()
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val (x, y, z) = event.values
                if (detector.onReading(x, y, z, event.timestamp / NANOS_PER_MS)) currentOnShake()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
        onPauseOrDispose { sensorManager?.unregisterListener(listener) }
    }
}
