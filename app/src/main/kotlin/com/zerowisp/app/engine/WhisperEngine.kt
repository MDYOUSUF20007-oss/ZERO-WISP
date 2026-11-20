package com.zerowisp.app.engine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * JNI bridge to libzerowisp_jni.so (whisper.cpp).
 * All native calls run on a dedicated single-thread dispatcher to avoid
 * multi-threaded access to the whisper context.
 *
 * CRITICAL: transcribeAudioNative returns jbyteArray (NOT jstring) to avoid
 * Android JNI Modified-UTF-8 crash with Bangla/multilingual tokens.
 */
class WhisperEngine {

    companion object {
        init {
            System.loadLibrary("zerowisp_jni")
        }

        val dispatcher: CoroutineDispatcher =
            Executors.newSingleThreadExecutor { r ->
                Thread(r, "whisper-inference-thread")
            }.asCoroutineDispatcher()

        val numThreads: Int
            get() = (Runtime.getRuntime().availableProcessors() / 2).coerceIn(2, 4)
    }

    // ── JNI declarations ────────────────────────────────────────────────────

    private external fun initContextNative(modelPath: String): Long
    private external fun freeContextNative(contextPtr: Long)
    private external fun transcribeAudioNative(
        contextPtr: Long,
        samples: FloatArray,
        translateToEnglish: Boolean,
        language: String,
        numThreads: Int
    ): ByteArray

    // ── Public API ───────────────────────────────────────────────────────────

    suspend fun initContext(modelPath: String): Long = withContext(dispatcher) {
        initContextNative(modelPath)
    }

    suspend fun freeContext(contextPtr: Long) = withContext(dispatcher) {
        if (contextPtr != 0L) {
            freeContextNative(contextPtr)
        }
    }

    suspend fun transcribeAudio(
        contextPtr: Long,
        samples: FloatArray,
        translateToEnglish: Boolean,
        language: String,
        numThreads: Int = Companion.numThreads
    ): String = withContext(dispatcher) {
        if (contextPtr == 0L || samples.isEmpty()) return@withContext ""
        val bytes = transcribeAudioNative(
            contextPtr, samples, translateToEnglish, language, numThreads
        )
        // Decode as UTF-8 — safe for Bangla, Hindi, Arabic, etc.
        String(bytes, Charsets.UTF_8)
    }
}
