package WTAY.screen_app_u22

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import WTAY.screen_app_u22.db.AppDatabase
import WTAY.screen_app_u22.db.AppUsageEntity
import java.util.Calendar

class DailyUsageWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val db = AppDatabase.getDatabase(appContext)
    private val usageHelper = UsageStatsHelper(appContext)

    override suspend fun doWork(): Result {
        return try {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startTime = calendar.timeInMillis
            val dateKey = startTime

            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            val endTime = calendar.timeInMillis

            val yesterdaysUsage = usageHelper.fetchAndAggregateUsage(startTime, endTime)

            yesterdaysUsage.forEach { item ->
                val entity = AppUsageEntity(
                    packageName = item.packageName,
                    appName = item.appName,
                    usageTime = item.usageTime,
                    date = dateKey
                )
                db.appUsageDao().insertOrUpdate(entity)
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}