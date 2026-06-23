package com.zhuogui.firewall.ui.apps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.zhuogui.firewall.databinding.FragmentAppListBinding
import com.zhuogui.firewall.ui.MainViewModel
import com.zhuogui.firewall.ui.adapter.AppAdapter
import com.zhuogui.firewall.ui.appdetail.AppDetailFragment
import kotlinx.coroutines.launch

class AppListFragment : Fragment() {

    private var _binding: FragmentAppListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels(ownerProducer = { requireActivity() })
    private lateinit var adapter: AppAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AppAdapter(
            onAppClick = { app ->
                // 点击 APP → 进入详情页
                navigateToDetail(app.packageName, app.appName)
            },
            onToggleBlock = { app, blocked ->
                viewModel.setAppAllowed(app.packageName, if (blocked) false else null)
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        var allAppsList = emptyList<com.zhuogui.firewall.data.entity.AppInfo>()
        var searchQuery = ""

        val updateFilteredList = {
            val filtered = if (searchQuery.isEmpty()) {
                allAppsList
            } else {
                allAppsList.filter {
                    it.appName.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
                }
            }
            adapter.submitList(filtered)
            binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allApps.collect { apps ->
                allAppsList = apps
                updateFilteredList()
            }
        }

        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchQuery = newText.orEmpty().trim()
                updateFilteredList()
                return true
            }
        })

        // 确保点击搜索框任何区域都能正常展开并弹出软键盘
        binding.searchView.setOnClickListener {
            binding.searchView.onActionViewExpanded()
            binding.searchView.requestFocus()
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.syncInstalledApps()
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun navigateToDetail(packageName: String, appName: String) {
        val fragment = AppDetailFragment.newInstance(packageName, appName)
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(com.zhuogui.firewall.R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
