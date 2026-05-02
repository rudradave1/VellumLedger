# SQLDelight rules
-keep class com.vellum.ledger.db.** { *; }

# Kotlin Serialization (if used in future)
-keepattributes *Annotation*, EnclosingMethod, Signature
-keepnames class kotlinx.serialization.internal.EnumDescriptor
-keepclassmembers class * {
    *** Companion;
}

# Keep Compose related classes
-keep class androidx.compose.** { *; }
