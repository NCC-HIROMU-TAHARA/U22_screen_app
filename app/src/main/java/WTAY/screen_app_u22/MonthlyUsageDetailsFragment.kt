package WTAY.screen_app_u22

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import WTAY.screen_app_u22.databinding.FragmentMonthlyUsageDetailsBinding // 新しいBindingクラスを生成
import kotlinx.coroutines.launch

class MonthlyUsageDetailsFragment : Fragment() {

    private var _binding: FragmentMonthlyUsageDetailsBinding? = null
    private val binding get() = _binding!!
    private lateinit var usageHelper: UsageStatsHelper

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMonthlyUsageDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        usageHelper = UsageStatsHelper(requireContext())
        setupRecyclerView()
        displayMonthlyUsageDetails()
    }

    private fun setupRecyclerView() {
        binding.monthlyUsageRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun displayMonthlyUsageDetails() {
        lifecycleScope.launch {
            val displayList = usageHelper.getMonthlyUsageFromDbAsync()

            val adapter = UsageListAdapter(requireContext(), displayList) { packageName ->
                val intent = requireContext().packageManager.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    startActivity(intent)
                } else {
                    Toast.makeText(requireContext(), "このアプリは起動できません", Toast.LENGTH_SHORT).show()
                }
            }
            binding.monthlyUsageRecyclerView.adapter = adapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}