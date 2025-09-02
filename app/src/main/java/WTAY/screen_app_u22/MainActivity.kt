package WTAY.screen_app_u22

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import WTAY.screen_app_u22.databinding.ActivityMainBinding // ★ ViewBindingのimportを追加
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var usageHelper: UsageStatsHelper
    private lateinit var binding: ActivityMainBinding // ★ bindingオブジェクトを宣言

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ★ ViewBindingを使ってレイアウトをセットアップ
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        usageHelper = UsageStatsHelper(this)
        setupUI()

        // ★ Toolbarのセットアップもbinding経由で行う
        setSupportActionBar(binding.toolbar)
    }

    override fun onResume() {
        super.onResume()
        checkPermissionAndLoadData()
    }

    private fun checkPermissionAndLoadData() {
        if (hasUsageStatsPermission()) {
            // ★ binding経由でビューを参照
            binding.buttonPermission.visibility = View.GONE
            // ScrollViewの表示/非表示も追加（権限ボタン復活のため）
            binding.contentScrollView.visibility = View.VISIBLE
            loadAndDisplayData()
        } else {
            // ★ binding経由でビューを参照
            binding.buttonPermission.visibility = View.VISIBLE
            binding.contentScrollView.visibility = View.GONE
        }
    }

    private fun setupUI() {
        // ★ クリックリスナーの設定もbinding経由で行う
        binding.buttonDaily.setOnClickListener {
            startActivity(Intent(this, DailyUsageDetailsActivity::class.java))
        }
        binding.buttonWeekly.setOnClickListener {
            startActivity(Intent(this, WeeklyUsageDetailsActivity::class.java))
        }
        binding.buttonMonthly.setOnClickListener {
            startActivity(Intent(this, MonthlyUsageDetailsActivity::class.java))
        }
        binding.buttonRefresh.setOnClickListener {
            checkPermissionAndLoadData()
        }
        binding.buttonPermission.setOnClickListener {
            requestUsageStatsPermission()
        }
    }

    private fun loadAndDisplayData() {
        binding.buttonRefresh.visibility = View.INVISIBLE
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // ★ binding経由でビューを参照
                val todaysTotal = usageHelper.getTodaysTotalUsage()
                binding.tvTodayTotal.text = usageHelper.formatDuration(todaysTotal)

                val cumulativeTotal = usageHelper.getCumulativeTotalUsage()
                binding.tvCumulativeTotal.text = usageHelper.formatDuration(cumulativeTotal)

                val mostLaunched = usageHelper.getMostLaunchedAppToday()
                if (mostLaunched != null && mostLaunched.launchCount > 0) {
                    binding.tvMostLaunchedAppName.text = "${mostLaunched.appName} (${mostLaunched.launchCount}回)"
                    binding.highlightCard.visibility = View.VISIBLE
                } else {
                    binding.highlightCard.visibility = View.GONE
                }
            } finally {
                binding.buttonRefresh.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_alert_settings -> {
                val intent = Intent(this, AlertSettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}