package WTAY.screen_app_u22

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import WTAY.screen_app_u22.databinding.FragmentUsageOverviewBinding

class UsageOverviewFragment : Fragment() {

    private var _binding: FragmentUsageOverviewBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUsageOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = UsagePagerAdapter(this) // UsagePagerAdapterは後で作成します
        binding.viewPager.adapter = adapter

        // TabLayoutとViewPager2を連携
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.daily_usage_tab_title)
                1 -> getString(R.string.weekly_usage_tab_title)
                2 -> getString(R.string.monthly_usage_tab_title)
                else -> null
            }
        }.attach()

        // 初期タブを「今日」に設定
        binding.viewPager.setCurrentItem(0, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}