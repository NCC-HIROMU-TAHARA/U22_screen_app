package WTAY.screen_app_u22

import android.app.Application
import androidx.work.*
import java.util.concurrent.TimeUnit

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupRecurringWork()
    }

    private fun setupRecurringWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresCharging(false)
            .build()

        // DailyUsageWorkerのスケジュール
        val dailyRequest = PeriodicWorkRequest.Builder(
            DailyUsageWorker::class.java,
            1,
            TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyUsageWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyRequest
        )

        // ▼▼▼ RecentUsageEventWorkerのスケジュール処理をすべて削除しました ▼▼▼
    }
}