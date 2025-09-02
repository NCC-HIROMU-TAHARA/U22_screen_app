package WTAY.screen_app_u22

import android.graphics.drawable.Drawable

data class AlertSettingItem(
    val packageName: String,
    val appName: String,
    val appIcon: Drawable,
    val usageLimitMillis: Long
)