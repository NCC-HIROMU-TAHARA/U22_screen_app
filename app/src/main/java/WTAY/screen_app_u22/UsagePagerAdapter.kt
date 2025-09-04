package WTAY.screen_app_u22
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
class UsagePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 3 // 日、週、月の3つのタブ

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> DailyUsageDetailsFragment()
            1 -> WeeklyUsageDetailsFragment() // WeeklyUsageDetailsFragmentは後で作成/修正します
            2 -> MonthlyUsageDetailsFragment() // MonthlyUsageDetailsFragmentは後で作成/修正します
            else -> throw IllegalStateException("Invalid position: $position")
        }
    }
}