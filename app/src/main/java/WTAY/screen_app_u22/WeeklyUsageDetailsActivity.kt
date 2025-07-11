package WTAY.screen_app_u22

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

class WeeklyUsageDetailsActivity : AppCompatActivity() {

    private lateinit var usageHelper: UsageStatsHelper
    private lateinit var weeklyUsageRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weekly_usage_details)

        val toolbar = findViewById<MaterialToolbar>(R.id.detailsToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "今週のアプリ利用履歴"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        usageHelper = UsageStatsHelper(this)
        weeklyUsageRecyclerView = findViewById(R.id.weeklyUsageRecyclerView)
        weeklyUsageRecyclerView.layoutManager = LinearLayoutManager(this)

        displayWeeklyUsageDetails()
    }

    private fun displayWeeklyUsageDetails() {
        lifecycleScope.launch {
            val displayList = usageHelper.getWeeklyUsageFromDbAsync()

            // ▼▼▼ アダプター作成時にクリック処理を渡す ▼▼▼
            val adapter = UsageListAdapter(this@WeeklyUsageDetailsActivity, displayList) { packageName ->
                val intent = packageManager.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    startActivity(intent)
                } else {
                    Toast.makeText(this@WeeklyUsageDetailsActivity, "このアプリは起動できません", Toast.LENGTH_SHORT).show()
                }
            }
            weeklyUsageRecyclerView.adapter = adapter
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}