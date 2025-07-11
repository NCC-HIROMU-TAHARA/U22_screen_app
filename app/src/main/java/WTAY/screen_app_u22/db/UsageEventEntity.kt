package WTAY.screen_app_u22.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class UsageEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val eventType: Int,
    val timestamp: Long
)