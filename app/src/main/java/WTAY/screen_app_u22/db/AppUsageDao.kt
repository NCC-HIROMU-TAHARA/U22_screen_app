package WTAY.screen_app_u22.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface AppUsageDao {

    @Upsert
    suspend fun upsertAll(appUsages: List<AppUsageEntity>)

    @Query("SELECT * FROM app_usage")
    suspend fun getAll(): List<AppUsageEntity>

    @Query("SELECT SUM(usageTime) FROM app_usage")
    suspend fun getTotalUsageTime(): Long

    // getAllをMap<String, Long>で返すヘルパーメソッド
    suspend fun getAllUsageMap(): Map<String, Long> {
        return getAll().associate { it.packageName to it.usageTime }
    }
}