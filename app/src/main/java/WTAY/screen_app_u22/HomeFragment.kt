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
import WTAY.screen_app_u22.databinding.FragmentHomeBinding // FragmentHomeBindingに変更
import kotlinx.coroutines.launch
import kotlin.jvm.java

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var usageHelper: UsageStatsHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
        // これらのボタンは、引き続き対応するActivityを起動します
        binding.buttonDaily.setOnClickListener {
            startActivity(Intent(requireContext(), DailyUsageDetailsActivity::class.java))
        }
        binding.buttonWeekly.setOnClickListener {
            startActivity(Intent(requireContext(), WeeklyUsageDetailsActivity::class.java))
        }
        binding.buttonMonthly.setOnClickListener {
            startActivity(Intent(requireContext(), MonthlyUsageDetailsActivity::class.java))
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

        viewLifecycleOwner.lifecycleScope.launch {
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