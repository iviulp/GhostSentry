package com.zhuogui.firewall.ui.rules

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.zhuogui.firewall.databinding.FragmentRulesBinding
import com.zhuogui.firewall.data.entity.FirewallRule
import com.zhuogui.firewall.ui.MainViewModel
import com.zhuogui.firewall.ui.adapter.RuleAdapter
import kotlinx.coroutines.launch

class RulesFragment : Fragment() {

    private var _binding: FragmentRulesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels(ownerProducer = { requireActivity() })
    private lateinit var adapter: RuleAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRulesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = RuleAdapter(
            onDeleteRule = { rule ->
                viewModel.deleteRule(rule)
            },
            onToggleRule = { rule ->
                viewModel.addRule(rule.copy(blocked = !rule.blocked))
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allRules.collect { rules ->
                adapter.submitList(rules)
                binding.tvEmpty.visibility = if (rules.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        binding.fabAddRule.setOnClickListener {
            showAddRuleDialog()
        }
    }

    private fun showAddRuleDialog() {
        val context = requireContext()

        val pkgInput = TextInputEditText(context).apply {
            hint = "APP 包名（* 表示所有 APP）"
            setText("*")
        }
        val pkgLayout = TextInputLayout(context).apply {
            addView(pkgInput)
            setPadding(48, 16, 48, 0)
        }

        val targetInput = TextInputEditText(context).apply {
            hint = "域名或 IP，如 example.com"
        }
        val targetLayout = TextInputLayout(context).apply {
            addView(targetInput)
            setPadding(48, 8, 48, 0)
        }

        val container = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(pkgLayout)
            addView(targetLayout)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("添加阻止规则")
            .setView(container)
            .setPositiveButton("添加") { _, _ ->
                val pkg = pkgInput.text?.toString()?.trim() ?: "*"
                val target = targetInput.text?.toString()?.trim() ?: ""
                if (target.isNotEmpty()) {
                    val type = if (target.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"))) "ip" else "domain"
                    val rule = FirewallRule(
                        packageName = if (pkg.isEmpty()) "*" else pkg,
                        target = target,
                        blocked = true,
                        type = type
                    )
                    viewModel.addRule(rule)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
