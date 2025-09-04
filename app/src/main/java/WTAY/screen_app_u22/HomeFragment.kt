package WTAY.screen_app_u22

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import WTAY.screen_app_u22.databinding.FragmentHomeBinding // 新しいBindingクラスを生成
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var usageHelper: UsageStatsHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        usageHelper = UsageStatsHelper(requireContext())
        setupUI()
    }

    override fun onResume() {
        super.onResume()
        checkPermissionAndLoadData()
    }

    private fun checkPermissionAndLoadData() {
        if (hasUsageStatsPermission()) {
            binding.buttonPermission.visibility = View.GONE
            binding.contentScrollView.visibility = View.VISIBLE
            loadAndDisplayData()
        } else {
            binding.buttonPermission.visibility = View.VISIBLE
            binding.contentScrollView.visibility = View.GONE
        }
    }

    private fun setupUI() {
        // MainActivityから移行したボタンのリスナー設定
        binding.buttonRefresh.setOnClickListener {
            checkPermissionAndLoadData()
        }
        binding.buttonPermission.setOnClickListener {
            requestUsageStatsPermission()
        }
        // Daily/Weekly/Monthlyへの遷移はBottomNavigationViewで直接DailyUsageDetailsFragmentなどへ移行するため、ここでは削除。
        // もしHomeFragmentから他の詳細画面へ直接遷移させたい場合は、Activityへコールバックを実装して、Activityがフラグメントを切り替えるようにする。
        // 今回の要件では、メイン画面と今日の利用詳細画面、アラーム画面がボトムナビから直接行けるため、ここでは削除します。
    }

    private fun loadAndDisplayData() {
        binding.buttonRefresh.visibility = View.INVISIBLE
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
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
        val appOps = requireContext().getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            requireContext().packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageStatsPermission() {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}