package WTAY.screen_app_u22

import android.Manifest
import android.app.ActivityManager
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var usageHelper: UsageStatsHelper
    private lateinit var dailyUsageDetailsButton: Button
    private lateinit var weeklyUsageDetailsButton: Button
    private lateinit var monthlyUsageDetailsButton: Button
    private lateinit var totalUsageTextView: TextView
    private lateinit var usageButton: Button
    private lateinit var permissionButton: Button
    private lateinit var alertSettingsButton: Button

    private lateinit var highlightCard: MaterialCardView
    private lateinit var mostLaunchedLayout: LinearLayout
    private lateinit var mostLaunchedAppName: TextView
    private lateinit var timeSlotMorning: TextView
    private lateinit var timeSlotDay: TextView
    private lateinit var timeSlotNight: TextView

    // 【追加】バックグラウンド記録を制御するトグルボタン
    private lateinit var trackingToggleButton: ToggleButton

    // 【追加】通知権限をリクエストするためのランチャー
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startTrackingService()
            } else {
                Toast.makeText(this, "通知権限が許可されませんでした", Toast.LENGTH_SHORT).show()
                trackingToggleButton.isChecked = false
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.mainToolbar)
        setSupportActionBar(toolbar)

        usageHelper = UsageStatsHelper(this)

        // findViewById
        dailyUsageDetailsButton = findViewById(R.id.dailyUsageDetailsButton)
        weeklyUsageDetailsButton = findViewById(R.id.weeklyUsageDetailsButton)
        monthlyUsageDetailsButton = findViewById(R.id.monthlyUsageDetailsButton)
        totalUsageTextView = findViewById(R.id.totalUsage)
        usageButton = findViewById(R.id.usageButton)
        permissionButton = findViewById(R.id.permissionButton)
        alertSettingsButton = findViewById(R.id.alertSettingsButton)
        highlightCard = findViewById(R.id.highlightCard)
        mostLaunchedLayout = findViewById(R.id.mostLaunchedLayout)
        mostLaunchedAppName = findViewById(R.id.mostLaunchedAppName)
        timeSlotMorning = findViewById(R.id.timeSlotMorning)
        timeSlotDay = findViewById(R.id.timeSlotDay)
        timeSlotNight = findViewById(R.id.timeSlotNight)
        // 【追加】ToggleButtonのfindViewById
        trackingToggleButton = findViewById(R.id.trackingToggleButton)

        // setOnClickListener
        dailyUsageDetailsButton.setOnClickListener { navigateWithPermissionCheck(DailyUsageDetailsActivity::class.java) }
        weeklyUsageDetailsButton.setOnClickListener { navigateWithPermissionCheck(WeeklyUsageDetailsActivity::class.java) }
        monthlyUsageDetailsButton.setOnClickListener { navigateWithPermissionCheck(MonthlyUsageDetailsActivity::class.java) }
        usageButton.setOnClickListener { if (hasUsageStatsPermission()) updateAndDisplayData() else requestUsageStatsPermission() }
        permissionButton.setOnClickListener { requestUsageStatsPermission() }
        alertSettingsButton.setOnClickListener { startActivity(Intent(this, AlertSettingsActivity::class.java)) }

        // 【追加】ToggleButtonのリスナー
        trackingToggleButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkPermissionsAndStartService()
            } else {
                stopTrackingService()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasUsageStatsPermission()) {
            permissionButton.visibility = View.GONE
            updateAndDisplayData()
        } else {
            permissionButton.visibility = View.VISIBLE
            totalUsageTextView.text = "累計使用時間：権限が必要です"
            highlightCard.visibility = View.GONE
        }
        // 【追加】サービスの実行状態をトグルボタンに反映
        trackingToggleButton.isChecked = isServiceRunning()
    }

    // --- 権限チェックとサービス管理 ---

    private fun checkPermissionsAndStartService() {
        if (!hasUsageStatsPermission()) {
            Toast.makeText(this, "まず「使用状況へのアクセス」を許可してください", Toast.LENGTH_LONG).show()
            requestUsageStatsPermission()
            trackingToggleButton.isChecked = false
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }

        startTrackingService()
    }

    private fun startTrackingService() {
        val serviceIntent = Intent(this, UsageTrackingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        Toast.makeText(this, "バックグラウンド記録を開始しました", Toast.LENGTH_SHORT).show()
    }

    private fun stopTrackingService() {
        stopService(Intent(this, UsageTrackingService::class.java))
        Toast.makeText(this, "バックグラウンド記録を停止しました", Toast.LENGTH_SHORT).show()
    }

    private fun isServiceRunning(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION") // この用途ではまだ有効
        return manager.getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == UsageTrackingService::class.java.name }
    }

    // --- データ表示とナビゲーション ---

    private fun updateAndDisplayData() {
        lifecycleScope.launch {
            totalUsageTextView.text = "累計使用時間：更新中..."
            highlightCard.visibility = View.GONE
            usageHelper.updateCumulativeUsage()
            val totalTime = usageHelper.getAllAppsTotalUsageTime()
            totalUsageTextView.text = "累計使用時間：${formatMillisToHoursMinutes(totalTime)}"
            val highlights = usageHelper.analyzeTodayHighlights()
            displayHighlights(highlights)
        }
    }

    private fun displayHighlights(highlights: TodayHighlight) { /* 既存のコードのまま */
        var isHighlightAvailable = false
        highlights.mostLaunchedApp?.let {
            if (it.launchCount > 0) {
                mostLaunchedAppName.text = "${it.appName} (${it.launchCount}回)"
                mostLaunchedLayout.visibility = View.VISIBLE
                isHighlightAvailable = true
            } else { mostLaunchedLayout.visibility = View.GONE }
        } ?: run { mostLaunchedLayout.visibility = View.GONE }
        val ts = highlights.timeSlotUsage
        ts.morning?.let {
            if(it.usageTime > 0) {
                timeSlotMorning.text = "朝：${it.appName} (${formatMillisToHoursMinutes(it.usageTime)})"
                timeSlotMorning.visibility = View.VISIBLE
                isHighlightAvailable = true
            } else { timeSlotMorning.visibility = View.GONE }
        } ?: run { timeSlotMorning.visibility = View.GONE }
        ts.day?.let {
            if(it.usageTime > 0) {
                timeSlotDay.text = "昼：${it.appName} (${formatMillisToHoursMinutes(it.usageTime)})"
                timeSlotDay.visibility = View.VISIBLE
                isHighlightAvailable = true
            } else { timeSlotDay.visibility = View.GONE }
        } ?: run { timeSlotDay.visibility = View.GONE }
        ts.night?.let {
            if(it.usageTime > 0) {
                timeSlotNight.text = "夜：${it.appName} (${formatMillisToHoursMinutes(it.usageTime)})"
                timeSlotNight.visibility = View.VISIBLE
                isHighlightAvailable = true
            } else { timeSlotNight.visibility = View.GONE }
        } ?: run { timeSlotNight.visibility = View.GONE }
        if (isHighlightAvailable) {
            highlightCard.visibility = View.VISIBLE
        }
    }

    private fun navigateWithPermissionCheck(activityClass: Class<*>) {
        if (hasUsageStatsPermission()) {
            startActivity(Intent(this, activityClass))
        } else {
            requestUsageStatsPermission()
        }
    }

    // --- ユーティリティ ---

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageStatsPermission() {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    private fun formatMillisToHoursMinutes(millis: Long): String { /* 既存のコードのまま */
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        return when {
            hours > 0 -> "${hours}時間 ${minutes}分"
            minutes > 0 -> "${minutes}分"
            millis > 0 -> "< 1分"
            else -> "0分"
        }
    }
}