package WTAY.screen_app_u22

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.OneTimeWorkRequestBuilder // ★ WorkManagerのimport文を追加
import androidx.work.WorkManager             // ★ WorkManagerのimport文を追加
import WTAY.screen_app_u22.databinding.ActivityAlertSettingsBinding
import WTAY.screen_app_u22.databinding.DialogTimePickerBinding
import java.util.concurrent.TimeUnit

class AlertSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertSettingsBinding
    private lateinit var adapter: AlertSettingsAdapter
    private var appSelectionDialog: AlertDialog? = null // ダイアログの参照を保持

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlertSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // FAB（追加ボタン）がクリックされたときの処理
        binding.fab.setOnClickListener {
            showAppSelectionDialog()
        }
        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        loadAndDisplayAlertSettings()
    }

    private fun setupRecyclerView() {
        adapter = AlertSettingsAdapter(emptyList()) { packageName ->
            showDeleteConfirmDialog(packageName)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun loadAndDisplayAlertSettings() {
        val allAlerts = AppPreferences.getAllAlerts()

        if (allAlerts.isEmpty()) {
            binding.recyclerView.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE

            val alertItems = allAlerts.mapNotNull { (packageName, limitMillis) ->
                try {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val appIcon = packageManager.getApplicationIcon(appInfo)
                    AlertSettingItem(packageName, appName, appIcon, limitMillis)
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            }.sortedBy { it.appName } // アプリ名でソート
            adapter.updateData(alertItems)
        }
    }

    private fun showDeleteConfirmDialog(packageName: String) {
        AlertDialog.Builder(this)
            .setTitle("設定の削除")
            .setMessage("このアラート設定を削除しますか？")
            .setPositiveButton("削除") { _, _ ->
                AppPreferences.removeAlert(packageName)
                loadAndDisplayAlertSettings()
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun getInstalledApps(): List<AppInfo> {
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        val installedApps = mutableListOf<AppInfo>()

        for (info in resolveInfos) {
            if (info.activityInfo.packageName != packageName) {
                installedApps.add(
                    AppInfo(
                        packageName = info.activityInfo.packageName,
                        appName = info.loadLabel(pm).toString(),
                        appIcon = info.loadIcon(pm)
                    )
                )
            }
        }
        return installedApps.sortedBy { it.appName }
    }

    private fun showAppSelectionDialog() {
        val appList = getInstalledApps()
        val alreadyConfiguredPackages = AppPreferences.getAllAlerts().keys
        val filteredAppList = appList.filter { it.packageName !in alreadyConfiguredPackages }

        if (filteredAppList.isEmpty()) {
            Toast.makeText(this, "設定可能なアプリがありません。", Toast.LENGTH_SHORT).show()
            return
        }

        val recyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@AlertSettingsActivity)
            adapter = AppSelectionAdapter(filteredAppList) { selectedApp ->
                appSelectionDialog?.dismiss()
                showTimePickerDialog(selectedApp)
            }
        }

        appSelectionDialog = AlertDialog.Builder(this)
            .setTitle("アプリを選択")
            .setView(recyclerView)
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun showTimePickerDialog(appInfo: AppInfo) {
        val dialogBinding = DialogTimePickerBinding.inflate(LayoutInflater.from(this))

        dialogBinding.hourPicker.minValue = 0
        dialogBinding.hourPicker.maxValue = 23
        dialogBinding.hourPicker.value = 0

        // 分のNumberPickerの設定 (1分刻み)
        dialogBinding.minutePicker.minValue = 0
        dialogBinding.minutePicker.maxValue = 59
        dialogBinding.minutePicker.value = 15

        AlertDialog.Builder(this)
            .setTitle("${appInfo.appName} の時間設定")
            .setView(dialogBinding.root)
            .setPositiveButton("設定") { _, _ ->
                val hours = dialogBinding.hourPicker.value
                // NumberPickerから直接値を取得するように修正
                val minutes = dialogBinding.minutePicker.value

                if (hours == 0 && minutes == 0) {
                    Toast.makeText(this, "時間は0より大きく設定してください。", Toast.LENGTH_SHORT).show()
                } else {
                    val limitMillis = TimeUnit.HOURS.toMillis(hours.toLong()) + TimeUnit.MINUTES.toMillis(minutes.toLong())

                    AppPreferences.setAlert(appInfo.packageName, limitMillis)

                    loadAndDisplayAlertSettings()
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }
}