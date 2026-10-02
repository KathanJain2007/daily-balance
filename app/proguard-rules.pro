# Room proguard rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Compose rules
-keep class androidx.compose.material3.** { *; }
