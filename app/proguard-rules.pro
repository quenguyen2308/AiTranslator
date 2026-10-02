# ProGuard rules for AiTranslator

# Keep GenerativeModel and related classes
-keep class com.google.ai.client.generativeai.** { *; }

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep AndroidX
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
