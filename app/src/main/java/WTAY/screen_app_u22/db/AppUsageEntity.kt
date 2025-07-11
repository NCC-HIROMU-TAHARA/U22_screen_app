package WTAY.screen_app_u22.db // ← ".db" を追加

import androidx.room.Entity

@Entity(primaryKeys = ["packageName", "date"])
data class AppUsageEntity(
    val packageName: String,
    val appName: String,
    val usageTime: Long,
    val date: Long
)