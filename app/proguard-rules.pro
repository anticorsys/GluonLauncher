# app/proguard-rules.pro

# Общие правила оптимизации
-keepattributes Signature, EnclosingMethod, InnerClasses, *Annotation*, SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * { @androidx.annotation.Keep *; }

# Исправлено: Защита моделей данных Gluon от разрушения при обфускации R8
-keep class com.gluon.launcher.core.data.** { *; }
-keepclassmembers class com.gluon.launcher.core.data.** { *; }

# Kotlin Serialization правила сборщика
-keepclassmembers class * { @kotlinx.serialization.SerialName <fields>; }
-keepnames class kotlinx.serialization.internal.GeneratedSerializer { *; }
-keepclassmembers class * { *** Companion; *** $serializer; }

# Правила для библиотек безопасности (EncryptedSharedPreferences)
-keep class androidx.security.crypto.** { *; }
-dontwarn com.google.crypto.tink.**

# Сохраняем все классы данных для корректной работы с PocketBase и Kotlin Serialization
-keep class com.gluon.launcher.core.data.** { *; }

# Kotlin Serialization
-keepattributes *Annotation*
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.** { *; }

# Общие правила для Retrofit и OkHttp
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Сохраняем модели рабочего стола для сериализации/обфускации
-keep class com.gluon.launcher.core.data.WorkspaceAppItem { *; }
-keep class com.gluon.launcher.core.data.WorkspaceWidgetItem { *; }