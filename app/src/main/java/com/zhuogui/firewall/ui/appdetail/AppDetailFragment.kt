package com.zhuogui.firewall.ui.appdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.zhuogui.firewall.data.entity.ConnectionLog
import com.zhuogui.firewall.data.entity.FirewallRule
import com.zhuogui.firewall.databinding.FragmentAppDetailBinding
import com.zhuogui.firewall.ui.MainViewModel
import com.zhuogui.firewall.ui.adapter.ConnectionAdapter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.net.InetAddress

/**
 * APP 详情页：展示单个 APP 的所有网络连接
 *
 * 三种显示模式：
 * - 域名模式：优先显示域名，无域名则显示 IP
 * - IP 模式：始终显示目标 IP
 * - 全部模式：同时显示域名和 IP
 *
 * 支持排序：按域名 / 按 IP / 按时间
 */
class AppDetailFragment : Fragment() {

    companion object {
        const val ARG_PACKAGE_NAME = "packageName"
        const val ARG_APP_NAME = "appName"

        fun newInstance(packageName: String, appName: String): AppDetailFragment {
            return AppDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PACKAGE_NAME, packageName)
                    putString(ARG_APP_NAME, appName)
                }
            }
        }
    }

    private var _binding: FragmentAppDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels(ownerProducer = { requireActivity() })
    private lateinit var adapter: ConnectionAdapter

    private var packageName: String = ""
    private var appName: String = ""

    // 当前显示模式: "domain" | "ip" | "all"
    private var currentMode: String = "domain"
    private val _displayMode = MutableStateFlow("domain")
    private val _deduplicate = MutableStateFlow(false)

    // 当前排序: "domain" | "ip" | "time"
    private val _sortMode = MutableStateFlow("time")
    private val _activeTab = MutableStateFlow(0) // 0 = 已允许, 1 = 已阻止

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        packageName = arguments?.getString(ARG_PACKAGE_NAME) ?: ""
        appName = arguments?.getString(ARG_APP_NAME) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvAppName.text = appName
        binding.tvPackageName.text = packageName

        // 返回按钮
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // 初始化 Adapter
        adapter = ConnectionAdapter(
            onBlock = { log -> 
                blockConnection(log)
                // 实时切换到“已阻止”选项卡
                binding.tabLayout.selectTab(binding.tabLayout.getTabAt(1))
            },
            onUnblock = { log -> 
                unblockConnection(log)
                // 实时切换到“已允许”选项卡
                binding.tabLayout.selectTab(binding.tabLayout.getTabAt(0))
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        // 设置模式切换
        setupModeChips()
        // 设置排序切换
        setupSortChips()

        // 设置去重过滤
        binding.chipDeduplicate.setOnCheckedChangeListener { _, isChecked ->
            _deduplicate.value = isChecked
        }

        // 监听选项卡切换
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                _activeTab.value = tab.position
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
        })

        // 观察连接数据，当模式、去重、排序、选项卡或数据变化时更新
        viewLifecycleOwner.lifecycleScope.launch {
            combine(
                viewModel.getConnectionsForPackage(packageName),
                _displayMode,
                _deduplicate,
                _sortMode,
                _activeTab
            ) { logs, mode, dedup, sort, activeTab ->
                // 根据选项卡过滤数据：0为已允许/失败/超时，1为被阻止(BLOCKED)
                val filteredByTab = logs.filter { 
                    if (activeTab == 1) {
                        it.blocked || it.status == "BLOCKED"
                    } else {
                        !it.blocked && it.status != "BLOCKED"
                    }
                }

                val processed = if (dedup) {
                    filteredByTab.distinctBy { it.destDomain ?: it.destIp }
                } else {
                    filteredByTab
                }
                applySort(processed, sort) to mode
            }.collectLatest { (sorted, mode) ->
                adapter.setDeduplicated(binding.chipDeduplicate.isChecked)
                adapter.setDisplayMode(mode)
                adapter.submitList(sorted)
                updateEmptyState(sorted)
            }
        }
    }

    /**
     * 设置三种显示模式切换
     */
    private fun setupModeChips() {
        binding.chipDomain.setOnClickListener { switchMode("domain") }
        binding.chipIp.setOnClickListener { switchMode("ip") }
        binding.chipAll.setOnClickListener { switchMode("all") }
        switchMode("domain") // 默认域名模式
    }

    private fun switchMode(mode: String) {
        currentMode = mode
        _displayMode.value = mode
        binding.chipDomain.isChecked = mode == "domain"
        binding.chipIp.isChecked = mode == "ip"
        binding.chipAll.isChecked = mode == "all"
    }

    /**
     * 设置排序切换
     */
    private fun setupSortChips() {
        binding.chipSortTime.setOnClickListener { switchSort("time") }
        binding.chipSortDomain.setOnClickListener { switchSort("domain") }
        binding.chipSortIp.setOnClickListener { switchSort("ip") }
        switchSort("time") // 默认按时间
    }

    private fun switchSort(sort: String) {
        _sortMode.value = sort
        binding.chipSortTime.isChecked = sort == "time"
        binding.chipSortDomain.isChecked = sort == "domain"
        binding.chipSortIp.isChecked = sort == "ip"
    }

    /**
     * 根据排序方式排序
     */
    private fun applySort(logs: List<ConnectionLog>, sort: String): List<ConnectionLog> {
        return when (sort) {
            "domain" -> logs.sortedWith(
                compareBy<ConnectionLog> { it.destDomain ?: it.destIp }
                    .thenByDescending { it.timestamp }
            )

            "ip" -> logs.sortedWith(
                compareBy<ConnectionLog> { ipToSortKey(it.destIp) }
                    .thenByDescending { it.timestamp }
            )

            "time" -> logs.sortedByDescending { it.timestamp }
            else -> logs
        }
    }

    /**
     * 将 IP 转为可排序的 Long 值
     */
    private fun ipToSortKey(ip: String): Long {
        return try {
            val addr = InetAddress.getByName(ip)
            val bytes = addr.address
            if (bytes.size == 4) {
                ((bytes[0].toLong() and 0xFF) shl 24) or
                        ((bytes[1].toLong() and 0xFF) shl 16) or
                        ((bytes[2].toLong() and 0xFF) shl 8) or
                        (bytes[3].toLong() and 0xFF)
            } else 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * 阻止连接：创建 FirewallRule
     */
    private fun blockConnection(log: ConnectionLog) {
        val target = log.destDomain ?: log.destIp
        val type = if (log.destDomain != null) "domain" else "ip"
        val rule = FirewallRule(
            packageName = packageName,
            target = target,
            blocked = true,
            type = type
        )
        viewModel.addRule(rule)
    }

    /**
     * 放开连接：删除对应的 FirewallRule
     */
    private fun unblockConnection(log: ConnectionLog) {
        val target = log.destDomain ?: log.destIp
        viewModel.deleteRuleByTarget(packageName, target)
    }

    private fun updateEmptyState(logs: List<ConnectionLog>) {
        binding.tvEmpty.visibility = if (logs.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (logs.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
