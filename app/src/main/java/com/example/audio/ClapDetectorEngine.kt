package com.example.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.sqrt

enum class ClapSensitivity(val displayName: String, val thresholdMultiplier: Float) {
    LOW("Low (Loud Claps)", 1.4f),
    MEDIUM("Medium (Standard)", 1.0f),
    HIGH("High (Gentle Claps)", 0.65f)
}

enum class ClapTriggerMode(val displayName: String) {
    DOUBLE_CLAP("Double Clap (Recommended)"),
    SINGLE_CLAP("Single Clap")
}

class ClapDetectorEngine(
    private val context: Context,
    private val onClapDetected: () -> Unit
) {
    companion object {
        private const val TAG = "ClapDetectorEngine"
        private const val SAMPLE_RATE = 22050
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BASE_CLAP_THRESHOLD = 9000 // Base peak amplitude threshold
    }

    private val isListening = AtomicBoolean(false)
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    var sensitivity: ClapSensitivity = ClapSensitivity.MEDIUM
    var triggerMode: ClapTriggerMode = ClapTriggerMode.DOUBLE_CLAP

    // State for clap temporal pattern recognition
    private var lastClapTime: Long = 0
    private var firstClapTimestamp: Long = 0
    private var clapCountInSequence: Int = 0

    // Callback for live sound amplitude for UI meters if needed
    var onAmplitudeUpdated: ((Int) -> Unit)? = null

    fun isRunning(): Boolean = isListening.get()

    fun startListening(): Boolean {
        if (isListening.get()) return true

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Cannot start ClapDetector: RECORD_AUDIO permission not granted")
            return false
        }

        try {
            val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
            val bufferSize = (minBufferSize * 2).coerceAtLeast(2048)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed")
                audioRecord?.release()
                audioRecord = null
                return false
            }

            audioRecord?.startRecording()
            isListening.set(true)

            recordingThread = Thread({
                processAudioStream(bufferSize)
            }, "ClapDetectorThread")
            recordingThread?.priority = Thread.NORM_PRIORITY
            recordingThread?.start()

            Log.i(TAG, "Clap detection started successfully.")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio recording for clap detector", e)
            stopListening()
            return false
        }
    }

    fun stopListening() {
        if (!isListening.getAndSet(false)) return

        try {
            audioRecord?.let {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        } finally {
            audioRecord = null
            recordingThread = null
            clapCountInSequence = 0
            firstClapTimestamp = 0
        }
        Log.i(TAG, "Clap detection stopped.")
    }

    private fun processAudioStream(bufferSize: Int) {
        val audioBuffer = ShortArray(bufferSize / 2)
        var backgroundNoiseFloor = 2000.0

        while (isListening.get()) {
            val record = audioRecord ?: break
            val readCount = record.read(audioBuffer, 0, audioBuffer.size)
            if (readCount <= 0) continue

            var maxPeak = 0
            var sumSquares = 0.0

            for (i in 0 until readCount) {
                val sample = abs(audioBuffer[i].toInt())
                if (sample > maxPeak) {
                    maxPeak = sample
                }
                sumSquares += (sample * sample).toDouble()
            }

            val rms = sqrt(sumSquares / readCount)

            // Dynamically update ambient noise floor with moving average
            backgroundNoiseFloor = (backgroundNoiseFloor * 0.96) + (rms * 0.04)

            // Notify amplitude listener if attached
            onAmplitudeUpdated?.let { listener ->
                mainHandler.post { listener(maxPeak) }
            }

            // Calculate dynamic threshold based on sensitivity and current noise floor
            val dynamicThreshold = ((BASE_CLAP_THRESHOLD * sensitivity.thresholdMultiplier)
                .coerceAtLeast(backgroundNoiseFloor.toFloat() * 3.5f))

            val currentTime = System.currentTimeMillis()

            // Transient spike condition (sharp high volume peak significantly above noise floor)
            if (maxPeak > dynamicThreshold && maxPeak > (backgroundNoiseFloor * 2.8)) {
                // Minimum debounce between consecutive sound spikes (120ms to ignore reverberation)
                if (currentTime - lastClapTime > 120) {
                    lastClapTime = currentTime
                    handleDetectedClapSpike(currentTime)
                }
            }

            // Timeout reset for double clap sequence if user waited too long (> 900ms)
            if (clapCountInSequence > 0 && currentTime - firstClapTimestamp > 950) {
                clapCountInSequence = 0
                firstClapTimestamp = 0
            }
        }
    }

    private fun handleDetectedClapSpike(timestamp: Long) {
        if (triggerMode == ClapTriggerMode.SINGLE_CLAP) {
            // Single clap mode: trigger immediately
            Log.i(TAG, "Single Clap detected! Triggering canvas open.")
            triggerCallback()
            return
        }

        // Double Clap Mode:
        if (clapCountInSequence == 0) {
            firstClapTimestamp = timestamp
            clapCountInSequence = 1
            Log.d(TAG, "First clap detected, waiting for 2nd clap...")
        } else if (clapCountInSequence == 1) {
            val timeDiff = timestamp - firstClapTimestamp
            // Valid second clap window: 160ms to 850ms
            if (timeDiff in 160..850) {
                Log.i(TAG, "Double Clap confirmed (${timeDiff}ms apart)! Triggering canvas open.")
                clapCountInSequence = 0
                firstClapTimestamp = 0
                triggerCallback()
            } else {
                firstClapTimestamp = timestamp
                clapCountInSequence = 1
            }
        }
    }

    private fun triggerCallback() {
        mainHandler.post {
            onClapDetected()
        }
    }
}
