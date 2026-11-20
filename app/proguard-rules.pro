# whisper.cpp JNI
-keep class com.zerowisp.app.engine.WhisperEngine { *; }
-keep class com.zerowisp.app.engine.WhisperEngine$* { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep all native methods
-keepclasseswithmembernames class * {
    native <methods>;
}
