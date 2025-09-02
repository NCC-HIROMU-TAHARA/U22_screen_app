package WTAY.screen_app_u22

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import WTAY.screen_app_u22.databinding.ItemAlertSettingBinding
import java.util.concurrent.TimeUnit

class AlertSettingsAdapter(
    private var items: List<AlertSettingItem>,
    private val onDeleteClick: (String) -> Unit // 削除ボタンが押されたときにpackageNameを渡すコールバック
) : RecyclerView.Adapter<AlertSettingsAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemAlertSettingBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAlertSettingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.appIcon.setImageDrawable(item.appIcon)
        holder.binding.appName.text = item.appName

        // ミリ秒を「〇時間〇分」の形式にフォーマットして表示
        val limitHours = TimeUnit.MILLISECONDS.toHours(item.usageLimitMillis)
        val limitMinutes = TimeUnit.MILLISECONDS.toMinutes(item.usageLimitMillis) % 60

        holder.binding.usageLimit.text = when {
            limitHours > 0 && limitMinutes > 0 -> "${limitHours}時間 ${limitMinutes}分/日 を超えたら通知"
            limitHours > 0 -> "${limitHours}時間/日 を超えたら通知"
            else -> "${limitMinutes}分/日 を超えたら通知"
        }

        // 削除ボタンのクリックリスナーを設定
        holder.binding.deleteButton.setOnClickListener {
            onDeleteClick(item.packageName)
        }
    }

    override fun getItemCount(): Int = items.size

    // RecyclerViewのリストを更新するためのメソッド
    fun updateData(newItems: List<AlertSettingItem>) {
        items = newItems
        notifyDataSetChanged() // データを更新して再描画を指示
    }
}