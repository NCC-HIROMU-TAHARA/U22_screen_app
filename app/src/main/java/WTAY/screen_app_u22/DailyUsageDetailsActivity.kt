package WTAY.screen_app_u22

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

class DailyUsageDetailsActivity : AppCompatActivity() {

    private lateinit var usageHelper: UsageStatsHelper
    private lateinit var dailyUsageRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daily_usage_details)

        val toolbar = findViewById<MaterialToolbar>(R.id.detailsToolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.title = "今日のアプリ利用履歴"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        usageHelper = UsageStatsHelper(this)
        dailyUsageRecyclerView = findViewById(R.id.dailyUsageRecyclerView)
        dailyUsageRecyclerView.layoutManager = LinearLayoutManager(this)

        displayDailyUsageDetails()
    }

    private fun displayDailyUsageDetails() {
        lifecycleScope.launch {
            val displayList = usageHelper.getDailyUsage()

            // ▼▼▼ アダプター作成時にクリック処理を渡す ▼▼▼
            val adapter = UsageListAdapter(this@DailyUsageDetailsActivity, displayList) { packageName ->
                val intent = packageManager.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    startActivity(intent)
                } else {
                    Toast.makeText(this@DailyUsageDetailsActivity, "このアプリは起動できません", Toast.LENGTH_SHORT).show()
                }
            }
            dailyUsageRecyclerView.adapter = adapter
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}