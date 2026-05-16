# Google Error Prone annotations - referenced by Tink (EncryptedSharedPreferences)
-dontwarn com.google.errorprone.annotations.**

# Tink - used by EncryptedSharedPreferences
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# SQLCipher
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-dontwarn net.sqlcipher.**

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keep class kotlinx.coroutines.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.vellum.ledger.**$$serializer { *; }
-keepclassmembers class com.vellum.ledger.** {
    *** Companion;
}
-keepclasseswithmembers class com.vellum.ledger.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# SQLDelight
-keep class com.vellum.ledger.db.** { *; }
-dontwarn com.squareup.sqldelight.**

# Keep data classes used in serialization
-keep class com.vellum.ledger.domain.** { *; }
-keep class com.vellum.ledger.data.** { *; }