package WTAY.screen_app_u22

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import WTAY.screen_app_u22.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFragment())
                    binding.toolbar.title = getString(R.string.home_title)
                    true
                }
                R.id.nav_usage_overview -> { // ここをnav_usage_overviewに変更
                    replaceFragment(UsageOverviewFragment()) // UsageOverviewFragmentをロード
                    binding.toolbar.title = getString(R.string.usage_overview_title) // タイトルも更新
                    true
                }
                R.id.nav_alert_settings -> {
                    replaceFragment(AlertSettingsFragment())
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // オプションメニューはBottomNavigationViewで管理するため、ここではfalseを返す
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }
}