package WTAY.screen_app_u22

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 「いつ、どのアプリを、どのくらいの時間使ったか」という利用イベントを記録するためのテーブル。
 * 時間帯別集計の元データとなる。
 */
@Entity(tableName = "app_usage_events")
data class AppUsageEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val usageDuration: Long, // このイベントでの利用時間（ミリ秒）
    val timestamp: Long      // 利用が終わった時点のタイムスタンプ
)