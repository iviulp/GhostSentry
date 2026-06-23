package com.zhuogui.firewall.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zhuogui.firewall.data.entity.FirewallRule
import com.zhuogui.firewall.databinding.ItemRuleBinding

class RuleAdapter(
    private val onDeleteRule: (FirewallRule) -> Unit,
    private val onToggleRule: (FirewallRule) -> Unit
) : ListAdapter<FirewallRule, RuleAdapter.RuleViewHolder>(RuleDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RuleViewHolder {
        val binding = ItemRuleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RuleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RuleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RuleViewHolder(private val binding: ItemRuleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(rule: FirewallRule) {
            binding.tvTarget.text = rule.target
            binding.tvType.text = rule.type.uppercase()
            binding.tvPackage.text = if (rule.packageName == "*") "所有 APP" else rule.packageName

            binding.switchBlocked.isChecked = rule.blocked
            binding.tvStatus.text = if (rule.blocked) "阻止" else "允许"

            binding.switchBlocked.setOnCheckedChangeListener(null)
            binding.switchBlocked.isChecked = rule.blocked
            binding.switchBlocked.setOnCheckedChangeListener { _, _ ->
                onToggleRule(rule)
            }

            binding.btnDelete.setOnClickListener {
                onDeleteRule(rule)
            }
        }
    }

    object RuleDiffCallback : DiffUtil.ItemCallback<FirewallRule>() {
        override fun areItemsTheSame(oldItem: FirewallRule, newItem: FirewallRule): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: FirewallRule, newItem: FirewallRule): Boolean =
            oldItem == newItem
    }
}
