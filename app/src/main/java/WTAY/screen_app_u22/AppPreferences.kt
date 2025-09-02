package WTAY.screen_app_u22

import android.content.Context
import android.content.SharedPreferences

object AppPreferences {
    private const val NAME = "AlertPrefs"
    private const val MODE = Context.MODE_PRIVATE
    private lateinit var preferences: SharedPreferences

    // アプリケーション起動時に一度だけ初期化する
    fun init(context: Context) {
        preferences = context.getSharedPreferences(NAME, MODE)
    }

    // アラート設定のキー（パッケージ名）と値（許容時間ミリ秒）を保存
    fun setAlert(packageName: String, usageLimitMillis: Long) {
        preferences.edit().putLong(packageName, usageLimitMillis).apply()
    }

    // アラート設定を削除
    fun removeAlert(packageName: String) {
        preferences.edit().remove(packageName).apply()
    }

    // 設定されているすべてのアラートを取得
    fun getAllAlerts(): Map<String, Long> {
        // SharedPreferences の getAll() は Map<String, *> を返すのでキャストする
        @Suppress("UNCHECKED_CAST")
        return preferences.all as Map<String, Long>
    }
}