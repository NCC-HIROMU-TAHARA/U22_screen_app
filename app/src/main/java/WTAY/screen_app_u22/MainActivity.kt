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
import androidx.fragment.app.Fragment
import WTAY.screen_app_u22.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        // BottomNavigationViewのセットアップ
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFragment())
                    binding.toolbar.title = getString(R.string.home_title) // ツールバーのタイトルを更新
                    true
                }
                R.id.nav_daily_usage -> {
                    replaceFragment(DailyUsageDetailsFragment()) // DailyUsageDetailsFragmentは別途作成
                    binding.toolbar.title = getString(R.string.daily_usage_title)
                    true
                }
                R.id.nav_alert_settings -> {
                    replaceFragment(AlertSettingsFragment()) // AlertSettingsFragmentは別途作成
                    binding.toolbar.title = getString(R.string.alert_settings_title)
                    true
                }
                else -> false
            }
        }

        // 初回起動時にHomeFragmentを表示
        if (savedInstanceState == null) {
            binding.bottomNavigation.selectedItemId = R.id.nav_home
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    // MainActivityに直接Permission関連のメソッドは不要になります。
    // HomeFragmentで処理するか、必要に応じてMainActivityで処理し、フラグメントに結果を渡す仕組みを検討してください。
    // この例ではHomeFragmentに移行していると仮定します。
    // hasUsageStatsPermission() や requestUsageStatsPermission() の呼び出しはHomeFragment内で行われるべきです。

    // 既存のオプションメニューは不要になるため削除します。
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // getMenuInflater().inflate(R.menu.main_menu, menu); // これをコメントアウトまたは削除
        return false // メニューはBottomNavigationViewで管理するため、ここではfalseを返す
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // ここでのメニューアイテム処理も不要になります
        return super.onOptionsItemSelected(item)
    }
}