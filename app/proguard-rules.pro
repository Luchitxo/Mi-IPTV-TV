# Gson y Modelos de Datos de la App
-keep class com.example.miiptvvodtv.** { *; }

# Retrofit & OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers interface * { @retrofit2.http.* <methods>; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn okio.**

# Coroutines
-keepnames class kotlinx.coroutines.** { *; }

# Media3 / ExoPlayer
-dontwarn androidx.media3.**
-keep class androidx.media3.** { *; }
