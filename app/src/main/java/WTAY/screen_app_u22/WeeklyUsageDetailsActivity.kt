package WTAY.screen_app_u22

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

class WeeklyUsageDetailsActivity : AppCompatActivity() {

    private lateinit var usageHelper: UsageStatsHelper // usageHelperを使用する
    private lateinit var weeklyUsageRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weekly_usage_details)

        val toolbar = findViewById<MaterialToolbar>(R.id.detailsToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "今週のアプリ利用履歴"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // ▼▼▼ usageHelperを初期化 ▼▼▼
        usageHelper = UsageStatsHelper(this)
        weeklyUsageRecyclerView = findViewById(R.id.weeklyUsageRecyclerView)
        weeklyUsageRecyclerView.layoutManager = LinearLayoutManager(this)

        displayWeeklyUsageDetails()
    }

    private fun displayWeeklyUsageDetails() {
        // ▼▼▼ UsageStatsHelper を使うシンプルなロジックに戻す ▼▼▼
        lifecycleScope.launch {
            // UsageStatsHelperから、整形済みの「今週の利用状況」リストを直接取得します。
            val displayList = usageHelper.getWeeklyUsageFromDbAsync()

            // アダプターに取得したリストをセットして表示します。
            val adapter = UsageListAdapter(this@WeeklyUsageDetailsActivity, displayList)
            weeklyUsageRecyclerView.adapter = adapter
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}