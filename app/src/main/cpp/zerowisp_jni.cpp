#include <jni.h>
#include <string>
#include <vector>
#include <cstring>
#include <android/log.h>

#define LOG_TAG "ZeroWisp"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#ifndef WHISPER_STUB_BUILD
#include "whisper.h"
#endif

extern "C" {

#ifdef WHISPER_STUB_BUILD
// ============================================================
// STUB implementations (when whisper.cpp not yet cloned)
// ============================================================

JNIEXPORT jlong JNICALL
Java_com_zerowisp_app_engine_WhisperEngine_initContextNative(
        JNIEnv *env, jobject /* this */, jstring modelPath) {
    LOGE("WhisperEngine: stub build — whisper.cpp not found");
    return 0L;
}

JNIEXPORT void JNICALL
Java_com_zerowisp_app_engine_WhisperEngine_freeContextNative(
        JNIEnv *env, jobject /* this */, jlong contextPtr) {
    // stub
}

JNIEXPORT jbyteArray JNICALL
Java_com_zerowisp_app_engine_WhisperEngine_transcribeAudioNative(
        JNIEnv *env, jobject /* this */,
        jlong contextPtr, jfloatArray samples,
        jboolean translateToEnglish, jstring language, jint numThreads) {
    const char* stub = "[STUB BUILD: whisper.cpp not found]";
    jsize len = (jsize)strlen(stub);
    jbyteArray result = env->NewByteArray(len);
    env->SetByteArrayRegion(result, 0, len, (jbyte*)stub);
    return result;
}

#else
// ============================================================
// REAL implementations
// ============================================================

JNIEXPORT jlong JNICALL
Java_com_zerowisp_app_engine_WhisperEngine_initContextNative(
        JNIEnv *env, jobject /* this */, jstring modelPath) {
    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    LOGI("WhisperEngine: loading model from %s", path);

    whisper_context_params cparams = whisper_context_default_params();
    cparams.use_gpu = false;

    whisper_context* ctx = whisper_init_from_file_with_params(path, cparams);
    env->ReleaseStringUTFChars(modelPath, path);

    if (ctx == nullptr) {
        LOGE("WhisperEngine: failed to load model");
        return 0L;
    }

    LOGI("WhisperEngine: model loaded successfully");
    return (jlong)(intptr_t)ctx;
}

JNIEXPORT void JNICALL
Java_com_zerowisp_app_engine_WhisperEngine_freeContextNative(
        JNIEnv *env, jobject /* this */, jlong contextPtr) {
    if (contextPtr != 0L) {
        whisper_context* ctx = (whisper_context*)(intptr_t)contextPtr;
        whisper_free(ctx);
        LOGI("WhisperEngine: context freed");
    }
}

JNIEXPORT jbyteArray JNICALL
Java_com_zerowisp_app_engine_WhisperEngine_transcribeAudioNative(
        JNIEnv *env, jobject /* this */,
        jlong contextPtr, jfloatArray samples,
        jboolean translateToEnglish, jstring languageStr, jint numThreads) {

    if (contextPtr == 0L) {
        const char* err = "";
        jbyteArray result = env->NewByteArray(0);
        return result;
    }

    whisper_context* ctx = (whisper_context*)(intptr_t)contextPtr;

    // Get samples
    jsize sampleCount = env->GetArrayLength(samples);
    jfloat* sampleData = env->GetFloatArrayElements(samples, nullptr);

    // Get language string
    const char* langCStr = env->GetStringUTFChars(languageStr, nullptr);
    std::string langStr(langCStr);
    env->ReleaseStringUTFChars(languageStr, langCStr);

    // Setup inference params
    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.n_threads        = (int)numThreads;
    params.translate        = (bool)translateToEnglish;
    params.no_timestamps    = true;
    params.single_segment   = false;
    params.print_special    = false;
    params.print_progress   = false;
    params.print_realtime   = false;
    params.print_timestamps = false;

    // Language: null means auto-detect
    if (langStr.empty() || langStr == "auto") {
        params.language = nullptr;
        params.detect_language = true;
    } else {
        params.language = langStr.c_str();
        params.detect_language = false;
    }

    // Greedy sampling (beam_size = 1)
    params.greedy.best_of = 1;

    // Run inference
    int result = whisper_full(ctx, params, sampleData, (int)sampleCount);
    env->ReleaseFloatArrayElements(samples, sampleData, JNI_ABORT);

    if (result != 0) {
        LOGE("WhisperEngine: whisper_full failed with code %d", result);
        jbyteArray empty = env->NewByteArray(0);
        return empty;
    }

    // Collect all segments into UTF-8 string
    int segmentCount = whisper_full_n_segments(ctx);
    std::string transcription;
    transcription.reserve(segmentCount * 32);

    for (int i = 0; i < segmentCount; ++i) {
        const char* text = whisper_full_get_segment_text(ctx, i);
        if (text != nullptr) {
            if (!transcription.empty() && transcription.back() != ' ') {
                transcription += ' ';
            }
            transcription += text;
        }
    }

    // CRITICAL: Return jbyteArray (not NewStringUTF) to avoid Modified-UTF-8 crash
    // with multilingual characters (Bangla, Hindi, etc.)
    jsize byteLen = (jsize)transcription.size();
    jbyteArray byteArray = env->NewByteArray(byteLen);
    if (byteLen > 0) {
        env->SetByteArrayRegion(byteArray, 0, byteLen,
            reinterpret_cast<const jbyte*>(transcription.c_str()));
    }

    LOGI("WhisperEngine: transcribed %d segments, %d bytes", segmentCount, byteLen);
    return byteArray;
}

#endif // WHISPER_STUB_BUILD

} // extern "C"
