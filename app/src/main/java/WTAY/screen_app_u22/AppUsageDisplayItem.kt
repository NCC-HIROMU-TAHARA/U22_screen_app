package WTAY.screen_app_u22

data class AppUsageDisplayItem(
    val packageName: String,
    val appName: String,
    val usageTime: Long,
    val launchCount: Int = 0
)