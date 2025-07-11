package WTAY.screen_app_u22

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class UsageTrackingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var usageHelper: UsageStatsHelper

    companion object {
        const val CHANNEL_ID = "UsageTrackingChannel"
        const val NOTIFICATION_ID = 1
        // ▼▼▼ このように変更 ▼▼▼
        private val TRACKING_INTERVAL_MINUTES = 60L
        private val TRACKING_INTERVAL_MS = TimeUnit.MINUTES.toMillis(TRACKING_INTERVAL_MINUTES)
    }

    override fun onCreate() {
        super.onCreate()
        usageHelper = UsageStatsHelper(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())

        Log.i("UsageTrackingService", "Service starting.")

        serviceScope.coroutineContext.cancelChildren() // 既存のタスクをキャンセル
        serviceScope.launch {
            while (isActive) { // コルーチンがアクティブな間ループ
                Log.d("UsageTrackingService", "Updating usage data in background...")
                try {
                    // UsageStatsHelperのメソッドを呼び出してデータを更新・保存
                    usageHelper.updateCumulativeUsage()
                } catch (e: Exception) {
                    Log.e("UsageTrackingService", "Error updating usage data", e)
                }
                delay(TRACKING_INTERVAL_MS)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel() // サービス破棄時にコルーチンをキャンセル
        Log.i("UsageTrackingService", "Service destroyed.")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("利用状況を記録中")
            .setContentText("アプリの利用状況をバックグラウンドで記録しています。")
            // 【修正箇所】ic_launcher から、新しく作成した通知用アイコンに変更します
            .setSmallIcon(R.drawable.ic_notification_tracking_24)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // 通知をユーザーが消せないようにする
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Usage Tracking Service Channel",
                NotificationManager.IMPORTANCE_MIN
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}