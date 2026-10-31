# Keep Room entities and DAOs
-keep class com.photocleaner.app.data.local.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
