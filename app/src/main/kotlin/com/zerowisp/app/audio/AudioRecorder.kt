package com.zerowisp.app.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * Captures 16kHz mono PCM audio, normalizes to FloatArray [-1.0, 1.0],
 * and emits live RMS amplitude for the bubble's mic ring animation.
 *
 * Also handles pause-based chunking:
 *  - Detects >= 1.5s of speech followed by ~1.5s of silence (RMS < silenceThreshold)
 *  - Hard slice at 25s to handle extremely long continuous speech
 *  - Dispatches each completed chunk via onChunkReady callback
 */
class AudioRecorder {

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        const val SILENCE_THRESHOLD = 0.015f    // RMS below this = silence
        const val SILENCE_DURATION_S = 1.5f     // seconds of silence to trigger chunk
        const val SPEECH_DURATION_S = 1.5f      // minimum speech before chunking
        const val HARD_SLICE_S = 25f            // hard slice limit in seconds
        private const val READ_BUFFER_SAMPLES = 1600 // 100ms at 16kHz
    }

    private val _rmsFlow = MutableStateFlow(0f)
    val rmsFlow: StateFlow<Float> = _rmsFlow

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    private var isRecording = false

    fun startRecording(
        onChunkReady: (FloatArray) -> Unit,
        onFinalBuffer: (FloatArray) -> Unit
    ) {
        if (isRecording) return

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, ENCODING)
            .coerceAtLeast(READ_BUFFER_SAMPLES * 2)

        @Suppress("MissingPermission")
        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            ENCODING,
            minBufferSize * 4
        )

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return
        }

        audioRecord = record
        isRecording = true
        record.startRecording()

        recordingJob = scope.launch {
            val buffer = ShortArray(READ_BUFFER_SAMPLES)
            val chunkBuffer = mutableListOf<Float>()

            var silenceSamples = 0
            var speechSamples = 0
            val silenceSamplesThreshold = (SILENCE_DURATION_S * SAMPLE_RATE).toInt()
            val speechSamplesMin = (SPEECH_DURATION_S * SAMPLE_RATE).toInt()
            val hardSliceSamples = (HARD_SLICE_S * SAMPLE_RATE).toInt()
            var hadSpeech = false

            while (isActive && isRecording) {
                val read = record.read(buffer, 0, buffer.size)
                if (read <= 0) continue

                // Normalize to float
                val floatChunk = FloatArray(read) { buffer[it] / 32768f }

                // Calculate RMS
                var sumSq = 0f
                for (s in floatChunk) sumSq += s * s
                val rms = sqrt(sumSq / floatChunk.size)
                _rmsFlow.value = rms

                chunkBuffer.addAll(floatChunk.toList())

                if (rms >= SILENCE_THRESHOLD) {
                    speechSamples += read
                    silenceSamples = 0
                    hadSpeech = true
                } else {
                    silenceSamples += read
                }

                val shouldSliceOnSilence = hadSpeech &&
                    speechSamples >= speechSamplesMin &&
                    silenceSamples >= silenceSamplesThreshold

                val shouldHardSlice = chunkBuffer.size >= hardSliceSamples

                if (shouldSliceOnSilence || shouldHardSlice) {
                    val chunk = chunkBuffer.toFloatArray()
                    chunkBuffer.clear()
                    silenceSamples = 0
                    speechSamples = 0
                    hadSpeech = false
                    if (chunk.isNotEmpty()) {
                        onChunkReady(chunk)
                    }
                }
            }

            // Flush remaining audio
            if (chunkBuffer.isNotEmpty()) {
                onFinalBuffer(chunkBuffer.toFloatArray())
            }
            _rmsFlow.value = 0f
        }
    }

    fun stopRecording() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null
        audioRecord?.apply {
            try {
                stop()
                release()
            } catch (_: Exception) {}
        }
        audioRecord = null
    }

    val isActive: Boolean get() = isRecording
}
