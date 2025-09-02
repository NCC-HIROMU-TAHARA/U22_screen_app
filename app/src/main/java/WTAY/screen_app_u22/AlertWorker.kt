package WTAY.screen_app_u22

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class AlertWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val usageHelper = UsageStatsHelper(appContext)

    override suspend fun doWork(): Result {
        return try {
            val alertSettings = AppPreferences.getAllAlerts()
            if (alertSettings.isEmpty()) {
                return Result.success()
            }

            val todaysUsage = usageHelper.getDailyUsage()
            val todaysUsageMap = todaysUsage.associateBy({ it.packageName }, { it.usageTime })

            alertSettings.forEach { (packageName, usageLimitMillis) ->
                val currentUsage = todaysUsageMap[packageName] ?: 0L

                if (currentUsage > usageLimitMillis) {
                    sendNotification(packageName, usageLimitMillis)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun sendNotification(packageName: String, usageLimitMillis: Long) {
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "USAGE_ALERT_CHANNEL"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "アプリ使用時間アラート",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "設定したアプリの使用時間を超えた場合に通知します。"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val appName = try {
            val appInfo = appContext.packageManager.getApplicationInfo(packageName, 0)
            appContext.packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }

        val limitMinutes = TimeUnit.MILLISECONDS.toMinutes(usageLimitMillis)

        val notification = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(R.drawable.ic_notification_tracking_24)
            .setContentTitle("使いすぎアラート")
            .setContentText("$appName の使用時間が ${limitMinutes}分 を超えました。")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(packageName.hashCode(), notification)
    }
}