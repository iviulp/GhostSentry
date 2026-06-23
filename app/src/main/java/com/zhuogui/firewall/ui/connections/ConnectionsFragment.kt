package com.zhuogui.firewall.ui.connections

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.zhuogui.firewall.databinding.FragmentConnectionsBinding
import com.zhuogui.firewall.ui.MainViewModel
import com.zhuogui.firewall.ui.adapter.ConnectionAdapter
import kotlinx.coroutines.launch

class ConnectionsFragment : Fragment() {

    private var _binding: FragmentConnectionsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by viewModels(ownerProducer = { requireActivity() })
    private lateinit var adapter: ConnectionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConnectionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ConnectionAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        // 观察实时连接
        viewLifecycleOwner.lifecycleScope.launch {
            com.zhuogui.firewall.service.FirewallVpnService.liveConnections.collect { connections ->
                adapter.submitList(connections)
                binding.tvEmpty.visibility = if (connections.isEmpty()) View.VISIBLE else View.GONE
                binding.tvCount.text = "共 ${connections.size} 条连接"
            }
        }

        binding.btnClear.setOnClickListener {
            viewModel.clearLogs()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
