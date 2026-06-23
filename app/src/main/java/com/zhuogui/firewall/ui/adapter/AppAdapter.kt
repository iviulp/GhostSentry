package com.zhuogui.firewall.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zhuogui.firewall.data.entity.AppInfo
import com.zhuogui.firewall.databinding.ItemAppBinding

class AppAdapter(
    private val onAppClick: (AppInfo) -> Unit,
    private val onToggleBlock: (AppInfo, Boolean) -> Unit
) : ListAdapter<AppInfo, AppAdapter.AppViewHolder>(AppDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AppViewHolder(private val binding: ItemAppBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppInfo) {
            binding.tvAppName.text = app.appName
            binding.tvPackageName.text = app.packageName

            val isBlocked = app.allowed == false
            binding.switchBlock.isChecked = isBlocked
            binding.tvStatus.text = if (isBlocked) "已阻止" else "允许"

            binding.switchBlock.setOnCheckedChangeListener(null)
            binding.switchBlock.isChecked = isBlocked
            binding.switchBlock.setOnCheckedChangeListener { _, checked ->
                onToggleBlock(app, checked)
            }

            binding.root.setOnClickListener {
                onAppClick(app)
            }
        }
    }

    object AppDiffCallback : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean =
            oldItem.packageName == newItem.packageName

        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean =
            oldItem == newItem
    }
}
