package WTAY.screen_app_u22.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AppUsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(appUsage: AppUsageEntity)

    @Query("SELECT * FROM AppUsageEntity WHERE date >= :startTime AND date <= :endTime")
    suspend fun getUsageForPeriod(startTime: Long, endTime: Long): List<AppUsageEntity>

    // ▼▼▼ [新設] 全てのAppUsageEntityを取得するメソッド ▼▼▼
    @Query("SELECT * FROM AppUsageEntity")
    suspend fun getAllUsage(): List<AppUsageEntity>
}