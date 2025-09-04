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
import WTAY.screen_app_u22.databinding.FragmentUsageTabBinding // FragmentUsageTabBindingに変更
import kotlinx.coroutines.launch

class UsageTabFragment : Fragment() {

    private var _binding: FragmentUsageTabBinding? = null
    private val binding get() = _binding!!
    private lateinit var usageHelper: UsageStatsHelper
    private lateinit var adapter: UsageListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentUsageTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        usageHelper = UsageStatsHelper(requireContext())
        setupRecyclerView()
        // Fragmentが作成されたときに、デフォルトで今日の使用状況を表示します
        displayDailyUsageDetails()
    }

    private fun setupRecyclerView() {
        adapter = UsageListAdapter(requireContext(), emptyList()) { packageName ->
            val intent = requireContext().packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                startActivity(intent)
            } else {
                Toast.makeText(requireContext(), "このアプリは起動できません", Toast.LENGTH_SHORT).show()
            }
        }
        binding.usageRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.usageRecyclerView.adapter = adapter
    }

    fun displayDailyUsageDetails() {
        // MainActivityのToolbarタイトルを更新
        (activity as? MainActivity)?.setToolbarTitle("今日のアプリ利用履歴")
        viewLifecycleOwner.lifecycleScope.launch {
            val displayList = usageHelper.getDailyUsage()
            adapter.updateData(displayList)
        }
    }

    // 必要に応じて、週次や月次の表示メソッドを追加できます
    /*
    fun displayWeeklyUsageDetails() {
        (activity as? MainActivity)?.setToolbarTitle("今週のアプリ利用履歴")
        viewLifecycleOwner.lifecycleScope.launch {
            val displayList = usageHelper.getWeeklyUsageFromDbAsync()
            adapter.updateData(displayList)
        }
    }

    fun displayMonthlyUsageDetails() {
        (activity as? MainActivity)?.setToolbarTitle("今月のアプリ利用履歴")
        viewLifecycleOwner.lifecycleScope.launch {
            val displayList = usageHelper.getMonthlyUsageFromDbAsync()
            adapter.updateData(displayList)
        }
    }
    */

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}