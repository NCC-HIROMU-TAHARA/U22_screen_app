package WTAY.screen_app_u22

import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import WTAY.screen_app_u22.db.AppDatabase
import java.util.*
import java.util.concurrent.TimeUnit

class UsageStatsHelper(private val context: Context) {

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val packageManager = context.packageManager
    private val db = AppDatabase.getDatabase(context)

    // ▼▼▼ [新設] 今日の合計利用時間を取得するメソッド ▼▼▼
    fun getTodaysTotalUsage(): Long {
        return getDailyUsage().sumOf { it.usageTime }
    }

    // ▼▼▼ [新設] DBから累計利用時間を取得するメソッド ▼▼▼
    suspend fun getCumulativeTotalUsage(): Long {
        // DBにある過去の全データと、今日のリアルタイムデータを合算する
        val historicalData = db.appUsageDao().getAllUsage() // 全データを取得するDAOメソッドが必要
        val todaysTotal = getTodaysTotalUsage()

        // 過去のデータは日ごとに記録されているので、重複を考慮せず合算
        val historicalTotal = historicalData.sumOf { it.usageTime }

        return historicalTotal + todaysTotal
    }

    // [機能7] 今日の最多起動アプリを取得するメソッド
    fun getMostLaunchedAppToday(): AppUsageDisplayItem? {
        val dailyUsage = getDailyUsage()
        return dailyUsage.maxByOrNull { it.launchCount }
    }

    // [今日] 今日の利用状況をリアルタイムで取得
    fun getDailyUsage(): List<AppUsageDisplayItem> {
        val cal = Calendar.getInstance()
        val endTime = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis
        return getUsageForPeriodFromApi(startTime, endTime)
    }

    // [今週] 週間の利用状況を取得
    suspend fun getWeeklyUsageFromDbAsync(): List<AppUsageDisplayItem> {
        val cal = Calendar.getInstance()
        val endTime = cal.timeInMillis
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis
        return getUsageForPeriodFromDbAndApi(startTime, endTime)
    }

    // [今月] 月間の利用状況を取得
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

    private fun getUsageForPeriodFromApi(startTime: Long, endTime: Long): List<AppUsageDisplayItem> {
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