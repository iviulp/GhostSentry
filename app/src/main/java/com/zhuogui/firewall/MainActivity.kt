package com.zhuogui.firewall

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.zhuogui.firewall.databinding.ActivityMainBinding
import com.zhuogui.firewall.service.FirewallVpnService
import com.zhuogui.firewall.ui.MainViewModel
import com.zhuogui.firewall.ui.apps.AppListFragment
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    companion object {
        private const val VPN_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        // 初始加载 APP 列表
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(binding.fragmentContainer.id, AppListFragment())
                .commit()
        }

        setupFab()
        observeVpnState()
    }

    private fun setupFab() {
        lifecycleScope.launch {
            FirewallVpnService.isRunning.collect { running ->
                if (running) {
                    binding.fabVpn.setImageResource(android.R.drawable.ic_media_pause)
                } else {
                    binding.fabVpn.setImageResource(android.R.drawable.ic_media_play)
                }
            }
        }

        binding.fabVpn.setOnClickListener {
            if (FirewallVpnService.isRunning.value) {
                stopVpn()
            } else {
                startVpn()
            }
        }
    }

    private fun startVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, VPN_REQUEST_CODE)
        } else {
            onActivityResult(VPN_REQUEST_CODE, RESULT_OK, null)
        }
    }

    private fun stopVpn() {
        val intent = Intent(this, FirewallVpnService::class.java).apply {
            action = "STOP"
        }
        stopService(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
            startService(Intent(this, FirewallVpnService::class.java))
        }
    }

    private fun observeVpnState() {
        lifecycleScope.launch {
            FirewallVpnService.isRunning.collect { running ->
                binding.tvVpnStatus.text = if (running) "VPN 运行中" else "VPN 已停止"
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menuProxySettings) {
            showProxyConfigDialog()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showProxyConfigDialog() {
        val config = FirewallVpnService.getProxyConfig()

        val context = this
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 0)
        }

        val hostInput = EditText(context).apply {
            hint = "代理地址 (如 127.0.0.1)"
            setText(config.host)
        }
        val portInput = EditText(context).apply {
            hint = "代理端口 (如 1080)"
            setText(config.port.toString())
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        val switchEnabled = SwitchCompat(context).apply {
            text = "启用上游代理"
            isChecked = config.enabled
        }

        container.addView(hostInput)
        container.addView(portInput)
        container.addView(switchEnabled)

        MaterialAlertDialogBuilder(context)
            .setTitle("SOCKS5 代理设置")
            .setMessage("配置上游 SOCKS5 代理，使流量通过现有 VPN/代理转发")
            .setView(container)
            .setPositiveButton("保存") { _, _ ->
                val host = hostInput.text.toString().trim()
                val port = portInput.text.toString().trim().toIntOrNull() ?: 1080
                val enabled = switchEnabled.isChecked
                FirewallVpnService.setProxyConfig(host, port, enabled)
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
