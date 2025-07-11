package WTAY.screen_app_u22

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
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
    private lateinit var buttonRefresh: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        usageHelper = UsageStatsHelper(this)

        setupUI()
    }

    override fun onResume() {
        super.onResume()
        if (hasUsageStatsPermission()) {
            buttonPermission.visibility = View.GONE
            loadAndDisplayData()
        } else {
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
        buttonRefresh = findViewById(R.id.button_refresh)
        progressBar = findViewById(R.id.progress_bar)

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
        buttonRefresh.setOnClickListener {
            if(hasUsageStatsPermission()) {
                loadAndDisplayData()
            }
        }
        buttonPermission.setOnClickListener {
            requestUsageStatsPermission()
        }
    }

    private fun loadAndDisplayData() {
        // ▼▼▼ UIの表示/非表示を切り替え ▼▼▼
        buttonRefresh.visibility = View.INVISIBLE
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
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
            } finally {
                // ▼▼▼ 処理完了後にUIを元に戻す ▼▼▼
                buttonRefresh.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
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