# RedOps Mobile - Proguard Rules

# Keep Room entity and DAO classes
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep Retrofit and Gson models
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep,allowobfuscation,allowshrinking class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**

# Keep Coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }

# Keep RedOps Models and Entities
-keep class com.redops.mobile.data.local.entity.** { *; }
-keep class com.redops.mobile.data.remote.dto.** { *; }
-keep class com.redops.mobile.domain.model.** { *; }
