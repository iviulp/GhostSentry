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
            binding.tvStatus.text = if (isBlocked) "已阻止联网" else "允许正常联网"
            binding.tvStatus.setTextColor(if (isBlocked) 0xFFD32F2F.toInt() else 0xFF2E7D32.toInt())

            val greenColor = 0xFF2E7D32.toInt()
            val lightGreen = 0xFFE8F5E9.toInt()
            val redColor = 0xFFC62828.toInt()
            val lightRed = 0xFFFFEBEE.toInt()
            val grayColor = 0xFF9E9E9E.toInt()
            val lightGray = 0xFFE0E0E0.toInt()

            if (isBlocked) {
                // 允许按钮：置灰
                binding.btnAllow.strokeColor = android.content.res.ColorStateList.valueOf(lightGray)
                binding.btnAllow.setTextColor(grayColor)
                binding.btnAllow.backgroundTintList = android.content.res.ColorStateList.valueOf(0x00000000)

                // 阻止按钮：红底红字
                binding.btnBlock.strokeColor = android.content.res.ColorStateList.valueOf(redColor)
                binding.btnBlock.setTextColor(redColor)
                binding.btnBlock.backgroundTintList = android.content.res.ColorStateList.valueOf(lightRed)
            } else {
                // 允许按钮：绿底绿字
                binding.btnAllow.strokeColor = android.content.res.ColorStateList.valueOf(greenColor)
                binding.btnAllow.setTextColor(greenColor)
                binding.btnAllow.backgroundTintList = android.content.res.ColorStateList.valueOf(lightGreen)

                // 阻止按钮：置灰
                binding.btnBlock.strokeColor = android.content.res.ColorStateList.valueOf(lightGray)
                binding.btnBlock.setTextColor(grayColor)
                binding.btnBlock.backgroundTintList = android.content.res.ColorStateList.valueOf(0x00000000)
            }

            binding.btnAllow.setOnClickListener {
                onToggleBlock(app, false)
            }
            binding.btnBlock.setOnClickListener {
                onToggleBlock(app, true)
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
