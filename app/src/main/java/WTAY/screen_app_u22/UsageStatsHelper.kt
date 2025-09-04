package WTAY.screen_app_u22

import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
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

    // ▼▼▼ 追加するロジックここから ▼▼▼
    // ホーム画面から起動できるアプリのパッケージ名リストを初回アクセス時に取得
    private val launchablePackages: Set<String> by lazy {
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        packageManager.queryIntentActivities(launcherIntent, 0)
            .map { it.activityInfo.packageName }
            .toSet()
    }

    // 現在のデフォルトランチャーアプリのパッケージ名を初回アクセス時に取得
    private val defaultLauncherPackageName: String? by lazy {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val defaultLauncherInfo = packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        defaultLauncherInfo?.activityInfo?.packageName
    }

    // アプリが利用状況リストに表示可能かどうかを判定する公開メソッド
    // AlertSettingsFragmentからも利用するためpublicにする
    fun isAppDisplayable(packageName: String): Boolean {
        // 1. 自分のアプリは表示しない
        if (packageName == context.packageName) return false

        // 2. デフォルトのランチャーアプリは表示しない
        if (packageName == defaultLauncherPackageName) return false

        // 3. ホーム画面から直接起動できないアプリは表示しない
        if (!launchablePackages.contains(packageName)) return false

        return true
    }
    // ▲▲▲ 追加するロジックここまで ▲▲▲

    suspend fun getTodaysTotalUsage(): Long {
        return getDailyUsage().sumOf { it.usageTime }
    }

    suspend fun getCumulativeTotalUsage(): Long {
        // DBからのデータもisAppDisplayableでフィルタリング
        val historicalData = db.appUsageDao().getAllUsage().filter { isAppDisplayable(it.packageName) }
        val todaysTotal = getTodaysTotalUsage() // これはgetDailyUsage()経由で既にフィルタリングされている
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
        // DBから取得した履歴データにもフィルタリングを適用
        val historicalData = db.appUsageDao().getUsageForPeriod(startTime, endTime)
            .filter { isAppDisplayable(it.packageName) } // ▼▼▼ ここにフィルタリングを追加 ▼▼▼

        val todaysData = getDailyUsage() // これはgetUsageForPeriodFromApi()経由で既にフィルタリングされている。
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

    fun getUsageForPeriodFromApi(startTime: Long, endTime: Long): List<AppUsageDisplayItem> {
        val usageStatsList: List<UsageStats> = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        // isAppDisplayable メソッドにフィルタリングロジックが統合されたため、
        // ここでの launchablePackages と defaultLauncherPackageName の再定義は不要になる。
        // ただし、launchCountMap の取得は必要。

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
            // ▼▼▼ isAppDisplayable を利用してフィルタリング ▼▼▼
            if (!isAppDisplayable(packageName)) {
                return@mapNotNull null
            }
            // ▲▲▲ フィルタリングここまで ▲▲▲
            try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
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