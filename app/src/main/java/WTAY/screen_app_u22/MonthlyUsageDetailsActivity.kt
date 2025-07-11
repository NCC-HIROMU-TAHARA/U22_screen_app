package WTAY.screen_app_u22

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch

class MonthlyUsageDetailsActivity : AppCompatActivity() {

    private lateinit var usageHelper: UsageStatsHelper
    private lateinit var monthlyUsageRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monthly_usage_details)

        val toolbar = findViewById<MaterialToolbar>(R.id.detailsToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "今月のアプリ利用履歴"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        usageHelper = UsageStatsHelper(this)
        monthlyUsageRecyclerView = findViewById(R.id.monthlyUsageRecyclerView)
        monthlyUsageRecyclerView.layoutManager = LinearLayoutManager(this)

        displayMonthlyUsageDetails()
    }

    private fun displayMonthlyUsageDetails() {
        lifecycleScope.launch {
            val displayList = usageHelper.getMonthlyUsageFromDbAsync()

            // ▼▼▼ アダプター作成時にクリック処理を渡す ▼▼▼
            val adapter = UsageListAdapter(this@MonthlyUsageDetailsActivity, displayList) { packageName ->
                val intent = packageManager.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    startActivity(intent)
                } else {
                    Toast.makeText(this@MonthlyUsageDetailsActivity, "このアプリは起動できません", Toast.LENGTH_SHORT).show()
                }
            }
            monthlyUsageRecyclerView.adapter = adapter
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}