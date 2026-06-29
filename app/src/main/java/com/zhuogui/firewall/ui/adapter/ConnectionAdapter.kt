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
    private val onItemClick: ((ConnectionLog) -> Unit)? = null,
    private var displayMode: String = "domain"
) : ListAdapter<ConnectionLog, ConnectionAdapter.ConnectionViewHolder>(ConnectionDiffCallback) {

    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private var isDeduplicated = false

    fun setDisplayMode(mode: String) {
        displayMode = mode
        notifyDataSetChanged()
    }

    fun setDeduplicated(dedup: Boolean) {
        isDeduplicated = dedup
        ConnectionDiffCallback.isDeduplicated = dedup
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

            // 阻止/连接状态指示与按钮高亮设计
            val statusColor = when (log.status) {
                "SUCCESS" -> 0xFF2E7D32.toInt() // 绿字
                "FAILED" -> 0xFFD32F2F.toInt() // 红字
                "TIMEOUT" -> 0xFFF57C00.toInt() // 橙字
                "BLOCKED" -> 0xFF7B1FA2.toInt() // 紫字
                else -> 0xFF2E7D32.toInt()
            }
            binding.tvProtocol.text = "${log.protocol} (${log.status})"
            binding.tvProtocol.setTextColor(statusColor)

            if (log.blocked || log.status == "BLOCKED") {
                binding.root.setBackgroundColor(0x157B1FA2.toInt()) // 浅紫/红底色标识被阻止
                binding.btnBlock.text = "放行"
                binding.btnBlock.setTextColor(0xFF2E7D32.toInt()) // 绿字
                binding.btnBlock.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFE8F5E9.toInt()) // 绿底
            } else {
                if (log.status == "FAILED" || log.status == "TIMEOUT") {
                    binding.root.setBackgroundColor(0x15D32F2F.toInt()) // 浅红底色标识访问超时或失败
                } else {
                    binding.root.setBackgroundColor(0x00000000)
                }
                binding.btnBlock.text = "禁用"
                binding.btnBlock.setTextColor(0xFFC62828.toInt()) // 红字
                binding.btnBlock.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFFFEBEE.toInt()) // 红底
            }

            // 禁用/放开 按钮
            binding.btnBlock.setOnClickListener {
                if (log.blocked || log.status == "BLOCKED") {
                    onUnblock?.invoke(log)
                } else {
                    onBlock?.invoke(log)
                }
            }

            // 条目点击查看详情
            binding.root.setOnClickListener {
                onItemClick?.invoke(log)
            }
        }
    }

    object ConnectionDiffCallback : DiffUtil.ItemCallback<ConnectionLog>() {
        var isDeduplicated = false

        override fun areItemsTheSame(oldItem: ConnectionLog, newItem: ConnectionLog): Boolean {
            return if (isDeduplicated) {
                // 去重模式下，相同的包名和目标地址视为同一项（防止频繁更新时闪烁）
                oldItem.packageName == newItem.packageName &&
                        (oldItem.destDomain ?: oldItem.destIp) == (newItem.destDomain ?: newItem.destIp)
            } else {
                oldItem.id == newItem.id
            }
        }

        override fun areContentsTheSame(oldItem: ConnectionLog, newItem: ConnectionLog): Boolean {
            return oldItem.blocked == newItem.blocked &&
                    oldItem.status == newItem.status &&
                    oldItem.destPort == newItem.destPort &&
                    oldItem.protocol == newItem.protocol &&
                    oldItem.timestamp == newItem.timestamp &&
                    oldItem.destIp == newItem.destIp &&
                    oldItem.destDomain == newItem.destDomain
        }
    }
}
