package WTAY.screen_app_u22

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppSelectionAdapter(
    private val appList: List<AppInfo>,
    private val onAppSelected: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppSelectionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appIcon: ImageView = view.findViewById(R.id.appIcon)
        val appName: TextView = view.findViewById(R.id.appName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_selection, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appInfo = appList[position]
        holder.appIcon.setImageDrawable(appInfo.appIcon)
        holder.appName.text = appInfo.appName
        holder.itemView.setOnClickListener {
            onAppSelected(appInfo)
        }
    }

    override fun getItemCount() = appList.size
}