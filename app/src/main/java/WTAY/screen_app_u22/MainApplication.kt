package WTAY.screen_app_u22

import android.app.Application
import androidx.work.*
import java.util.concurrent.TimeUnit

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // AppPreferencesの初期化
        AppPreferences.init(this)

        // WorkManagerのインスタンスを取得
        val workManager = WorkManager.getInstance(this)

        // 1. 昨日の利用履歴を保存するDailyUsageWorkerをスケジュール (既存の処理)
        val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyUsageWorker>(1, TimeUnit.DAYS)
            .build() // ネットワーク制約はバッテリー消費に影響するため、一旦削除してシンプルに

        workManager.enqueueUniquePeriodicWork(
            "dailyUsageLog",
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWorkRequest
        )

        // ▼▼▼ このブロックをまるごと追加 ▼▼▼
        // 2. アプリの使いすぎを監視するAlertWorkerをスケジュール (不足していた処理)
        val alertWorkRequest = PeriodicWorkRequestBuilder<AlertWorker>(
            15, TimeUnit.MINUTES // WorkManagerの最短間隔である15分ごとに実行
        ).build()

        workManager.enqueueUniquePeriodicWork(
            "appUsageAlert",
            ExistingPeriodicWorkPolicy.KEEP,
            alertWorkRequest
        )
        // ▲▲▲ ここまで追加 ▲▲▲
    }
}