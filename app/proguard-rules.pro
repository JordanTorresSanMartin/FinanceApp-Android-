# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# Stack traces legibles en crashes de release.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Modelos @Serializable (kotlinx.serialization trae reglas, esto cubre los nombres usados por Supabase).
-keep,includedescriptorclasses class com.example.financeapp.data.model.**$$serializer { *; }
-keepclassmembers class com.example.financeapp.data.model.** { *** Companion; }
-keepclasseswithmembers class com.example.financeapp.data.model.** { kotlinx.serialization.KSerializer serializer(...); }
# Rutas de navegación type-safe (@Serializable)
-keep class com.example.financeapp.ui.navigation.NavRoute** { *; }
-dontwarn org.slf4j.**

# WorkManager (aviso en vivo de pagos): Room crea WorkDatabase_Impl por reflexión.
# Sin esto, R8 borra su constructor y la app se cae al abrir en release.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
