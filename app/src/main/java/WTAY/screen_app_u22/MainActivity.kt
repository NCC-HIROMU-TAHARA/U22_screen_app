package WTAY.screen_app_u22

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var usageHelper: UsageStatsHelper

    // UI elements
    private lateinit var tvTodayTotal: TextView
    private lateinit var tvCumulativeTotal: TextView
    private lateinit var highlightCard: MaterialCardView
    private lateinit var tvMostLaunchedAppName: TextView
    private lateinit var buttonPermission: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        usageHelper = UsageStatsHelper(this)

        setupUI()
    }

    override fun onResume() {
        super.onResume()
        // 権限を確認してUIを更新
        if (hasUsageStatsPermission()) {
            buttonPermission.visibility = View.GONE
            loadAndDisplayData()
        } else {
            // 権限がない場合のUI表示
            buttonPermission.visibility = View.VISIBLE
            tvTodayTotal.text = "- 時間 - 分"
            tvCumulativeTotal.text = "- 時間 - 分"
            highlightCard.visibility = View.GONE
        }
    }

    private fun setupUI() {
        // Find views
        tvTodayTotal = findViewById(R.id.tv_today_total)
        tvCumulativeTotal = findViewById(R.id.tv_cumulative_total)
        highlightCard = findViewById(R.id.highlight_card)
        tvMostLaunchedAppName = findViewById(R.id.tv_most_launched_app_name)
        buttonPermission = findViewById(R.id.button_permission)

        // Set click listeners
        findViewById<Button>(R.id.button_daily).setOnClickListener {
            startActivity(Intent(this, DailyUsageDetailsActivity::class.java))
        }
        findViewById<Button>(R.id.button_weekly).setOnClickListener {
            startActivity(Intent(this, WeeklyUsageDetailsActivity::class.java))
        }
        findViewById<Button>(R.id.button_monthly).setOnClickListener {
            startActivity(Intent(this, MonthlyUsageDetailsActivity::class.java))
        }
        findViewById<Button>(R.id.button_refresh).setOnClickListener {
            if(hasUsageStatsPermission()) {
                loadAndDisplayData()
            }
        }
        buttonPermission.setOnClickListener {
            requestUsageStatsPermission()
        }
    }

    private fun loadAndDisplayData() {
        lifecycleScope.launch {
            // 今日の合計時間を取得して表示
            val todaysTotal = usageHelper.getTodaysTotalUsage()
            tvTodayTotal.text = usageHelper.formatDuration(todaysTotal)

            // 累計時間を取得して表示
            val cumulativeTotal = usageHelper.getCumulativeTotalUsage()
            tvCumulativeTotal.text = usageHelper.formatDuration(cumulativeTotal)

            // 最多起動アプリを取得して表示
            val mostLaunched = usageHelper.getMostLaunchedAppToday()
            if (mostLaunched != null && mostLaunched.launchCount > 0) {
                tvMostLaunchedAppName.text = "${mostLaunched.appName} (${mostLaunched.launchCount}回)"
                highlightCard.visibility = View.VISIBLE
            } else {
                highlightCard.visibility = View.GONE
            }
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageStatsPermission() {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }
}