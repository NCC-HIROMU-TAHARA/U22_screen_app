package WTAY.screen_app_u22

import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import WTAY.screen_app_u22.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.TimeUnit

class UsageStatsHelper(private val context: Context) {

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val packageManager = context.packageManager
    private val db = AppDatabase.getDatabase(context)

    suspend fun getTodaysTotalUsage(): Long {
        return getDailyUsage().sumOf { it.usageTime }
    }

    suspend fun getCumulativeTotalUsage(): Long {
        val historicalData = db.appUsageDao().getAllUsage()
        val todaysTotal = getTodaysTotalUsage()
        val historicalTotal = historicalData.sumOf { it.usageTime }
        return historicalTotal + todaysTotal
    }

    suspend fun getMostLaunchedAppToday(): AppUsageDisplayItem? {
        val dailyUsage = getDailyUsage()
        return dailyUsage.maxByOrNull { it.launchCount }
    }

    suspend fun getDailyUsage(): List<AppUsageDisplayItem> {
        return withContext(Dispatchers.IO) {
            val cal = Calendar.getInstance()
            val endTime = cal.timeInMillis
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            val startTime = cal.timeInMillis
            getUsageForPeriodFromApi(startTime, endTime)
        }
    }

    suspend fun getWeeklyUsageFromDbAsync(): List<AppUsageDisplayItem> {
        val cal = Calendar.getInstance()
        val endTime = cal.timeInMillis
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis
        return getUsageForPeriodFromDbAndApi(startTime, endTime)
    }

    suspend fun getMonthlyUsageFromDbAsync(): List<AppUsageDisplayItem> {
        val cal = Calendar.getInstance()
        val endTime = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis
        return getUsageForPeriodFromDbAndApi(startTime, endTime)
    }

    private suspend fun getUsageForPeriodFromDbAndApi(startTime: Long, endTime: Long): List<AppUsageDisplayItem> {
        val historicalData = db.appUsageDao().getUsageForPeriod(startTime, endTime)
        val todaysData = getDailyUsage()
        val aggregatedStats = mutableMapOf<String, Pair<String, Long>>()

        historicalData.forEach { entity ->
            val currentTotal = aggregatedStats[entity.packageName]?.second ?: 0L
            aggregatedStats[entity.packageName] = Pair(entity.appName, currentTotal + entity.usageTime)
        }

        todaysData.forEach { item ->
            val currentTotal = aggregatedStats[item.packageName]?.second ?: 0L
            aggregatedStats[item.packageName] = Pair(item.appName, currentTotal + item.usageTime)
        }
        return aggregatedStats.map { (packageName, data) ->
            AppUsageDisplayItem(packageName = packageName, appName = data.first, usageTime = data.second)
        }.sortedByDescending { it.usageTime }
    }

    fun fetchAndAggregateUsage(startTime: Long, endTime: Long): List<AppUsageDisplayItem> {
        return getUsageForPeriodFromApi(startTime, endTime)
    }

    // ▼▼▼ ここの `private` を削除しました ▼▼▼
    fun getUsageForPeriodFromApi(startTime: Long, endTime: Long): List<AppUsageDisplayItem> {
        val usageStatsList: List<UsageStats> = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        val launchCountMap = mutableMapOf<String, Int>()
        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                val currentCount = launchCountMap.getOrDefault(event.packageName, 0)
                launchCountMap[event.packageName] = currentCount + 1
            }
        }

        val aggregatedStats = mutableMapOf<String, Long>()
        for (usageStats in usageStatsList) {
            val currentTotal = aggregatedStats.getOrDefault(usageStats.packageName, 0L)
            aggregatedStats[usageStats.packageName] = currentTotal + usageStats.totalTimeInForeground
        }

        return aggregatedStats.mapNotNull { (packageName, totalTime) ->
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                if (packageName == context.packageName) return@mapNotNull null
                val appName = packageManager.getApplicationLabel(appInfo).toString()
                if (totalTime > 0) {
                    val launchCount = launchCountMap[packageName] ?: 0
                    AppUsageDisplayItem(packageName = packageName, appName = appName, usageTime = totalTime, launchCount = launchCount)
                } else {
                    null
                }
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        }.sortedByDescending { it.usageTime }
    }

    fun formatDuration(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        return when {
            hours > 0 -> "${hours}時間 ${minutes}分"
            minutes > 0 -> "${minutes}分"
            else -> "< 1分"
        }
    }
}