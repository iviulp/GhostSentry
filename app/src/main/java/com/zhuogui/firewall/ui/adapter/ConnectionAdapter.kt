package com.zhuogui.firewall.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zhuogui.firewall.data.entity.ConnectionLog
import com.zhuogui.firewall.databinding.ItemConnectionBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 连接日志 Adapter
 *
 * 支持三种显示模式：
 * - 域名模式：优先显示 destDomain，无则显示 destIp
 * - IP 模式：始终显示 destIp
 * - 全部模式：同时显示域名和 IP
 *
 * 每条连接右侧有 禁用/放开 按钮
 */
class ConnectionAdapter(
    private val onBlock: ((ConnectionLog) -> Unit)? = null,
    private val onUnblock: ((ConnectionLog) -> Unit)? = null,
    private var displayMode: String = "domain"
) : ListAdapter<ConnectionLog, ConnectionAdapter.ConnectionViewHolder>(ConnectionDiffCallback) {

    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun setDisplayMode(mode: String) {
        displayMode = mode
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConnectionViewHolder {
        val binding = ItemConnectionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ConnectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConnectionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ConnectionViewHolder(private val binding: ItemConnectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(log: ConnectionLog) {
            // APP 名称
            binding.tvAppName.text = log.appName
            binding.tvPackage.text = log.packageName

            // 根据显示模式展示目标地址
            val targetDisplay = when (displayMode) {
                "domain" -> log.destDomain ?: log.destIp
                "ip" -> log.destIp
                "all" -> {
                    if (log.destDomain != null) {
                        "${log.destDomain} → ${log.destIp}"
                    } else {
                        log.destIp
                    }
                }

                else -> log.destDomain ?: log.destIp
            }

            binding.tvTarget.text = "$targetDisplay:${log.destPort}"
            binding.tvProtocol.text = log.protocol
            binding.tvTime.text = dateFormat.format(Date(log.timestamp))

            // 阻止状态指示
            if (log.blocked) {
                binding.root.setBackgroundColor(0x20FF0000.toInt())
                binding.btnBlock.text = "放开"
                binding.btnBlock.setTextColor(0xFF4CAF50.toInt())
            } else {
                binding.root.setBackgroundColor(0x00000000)
                binding.btnBlock.text = "禁用"
                binding.btnBlock.setTextColor(0xFFFF4444.toInt())
            }

            // 禁用/放开 按钮
            binding.btnBlock.setOnClickListener {
                if (log.blocked) {
                    onUnblock?.invoke(log)
                } else {
                    onBlock?.invoke(log)
                }
            }
        }
    }

    object ConnectionDiffCallback : DiffUtil.ItemCallback<ConnectionLog>() {
        override fun areItemsTheSame(oldItem: ConnectionLog, newItem: ConnectionLog): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ConnectionLog, newItem: ConnectionLog): Boolean =
            oldItem == newItem
    }
}
