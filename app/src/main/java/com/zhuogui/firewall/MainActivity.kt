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

    private val vpnLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startVpnService()
        }
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

        setupTogglePanel()
    }

    private fun setupTogglePanel() {
        lifecycleScope.launch {
            FirewallVpnService.isRunning.collect { running ->
                if (running) {
                    binding.tvVpnStatus.text = "正在安全保护中"
                    binding.tvVpnStatus.setTextColor(0xFF2E7D32.toInt()) // 绿色
                    binding.btnToggleVpn.text = "关闭防护"
                    binding.btnToggleVpn.setBackgroundColor(0xFFD32F2F.toInt()) // 红色
                } else {
                    binding.tvVpnStatus.text = "防护已关闭"
                    binding.tvVpnStatus.setTextColor(0xFFD32F2F.toInt()) // 红色
                    binding.btnToggleVpn.text = "开启防护"
                    binding.btnToggleVpn.setBackgroundColor(0xFF2E7D32.toInt()) // 绿色
                }
            }
        }

        binding.btnToggleVpn.setOnClickListener {
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
            vpnLauncher.launch(intent)
        } else {
            startVpnService()
        }
    }

    private fun startVpnService() {
        val intent = Intent(this, FirewallVpnService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopVpn() {
        val intent = Intent(this, FirewallVpnService::class.java).apply {
            action = "STOP"
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
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
            setPadding(48, 16, 48, 16)
        }

        // 代理类型选择
        val radioGroup = android.widget.RadioGroup(context).apply {
            orientation = android.widget.RadioGroup.HORIZONTAL
            setPadding(0, 0, 0, 16)
        }

        val radioLocal = android.widget.RadioButton(context).apply {
            text = "本机代理 (127.0.0.1)"
            id = android.view.View.generateViewId()
        }

        val radioExternal = android.widget.RadioButton(context).apply {
            text = "外部代理"
            id = android.view.View.generateViewId()
        }

        radioGroup.addView(radioLocal)
        radioGroup.addView(radioExternal)

        val hostInput = EditText(context).apply {
            hint = "代理地址 (如 192.168.1.100)"
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
            setPadding(0, 16, 0, 16)
        }

        val selectedPackageName = java.util.concurrent.atomic.AtomicReference(config.proxyPackage)
        val packageText = android.widget.TextView(context).apply {
            text = if (selectedPackageName.get().isEmpty()) "未选择本地代理应用 (外部代理无需选择)" else "已选代理应用: ${selectedPackageName.get()}"
            setPadding(0, 8, 0, 16)
            textSize = 14f
        }

        val selectAppBtn = com.google.android.material.button.MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "选择本地代理应用"
            setOnClickListener {
                val pm = packageManager
                val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
                val apps = resolveInfos.map {
                    val appName = it.loadLabel(pm).toString()
                    val packageName = it.activityInfo.packageName
                    appName to packageName
                }.distinctBy { it.second }.sortedBy { it.first.lowercase() }

                val appNames = apps.map { "${it.first} (${it.second})" }.toTypedArray()

                MaterialAlertDialogBuilder(context)
                    .setTitle("选择本地代理应用")
                    .setItems(appNames) { _, which ->
                        selectedPackageName.set(apps[which].second)
                        packageText.text = "已选代理应用: ${apps[which].first} (${selectedPackageName.get()})"
                    }
                    .setNeutralButton("清除选择") { _, _ ->
                        selectedPackageName.set("")
                        packageText.text = "未选择本地代理应用 (外部代理无需选择)"
                    }
                    .show()
            }
        }

        // 动态显隐逻辑
        val updateVisibility = { isLocal: Boolean ->
            if (isLocal) {
                hostInput.setText("127.0.0.1")
                hostInput.isEnabled = false
                packageText.visibility = android.view.View.VISIBLE
                selectAppBtn.visibility = android.view.View.VISIBLE
            } else {
                if (hostInput.text.toString() == "127.0.0.1" || hostInput.text.toString() == "localhost") {
                    hostInput.setText("")
                }
                hostInput.isEnabled = true
                packageText.visibility = android.view.View.GONE
                selectAppBtn.visibility = android.view.View.GONE
            }
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            updateVisibility(checkedId == radioLocal.id)
        }

        // 初始化选中状态
        val isInitiallyLocal = config.host == "127.0.0.1" || config.host == "localhost" || config.host == "::1" || config.proxyPackage.isNotEmpty()
        if (isInitiallyLocal) {
            radioGroup.check(radioLocal.id)
            updateVisibility(true)
        } else {
            radioGroup.check(radioExternal.id)
            updateVisibility(false)
        }

        container.addView(radioGroup)
        container.addView(hostInput)
        container.addView(portInput)
        container.addView(switchEnabled)
        container.addView(packageText)
        container.addView(selectAppBtn)

        MaterialAlertDialogBuilder(context)
            .setTitle("SOCKS5 代理设置")
            .setMessage("配置上游 SOCKS5 代理，使流量通过现有 VPN/代理转发")
            .setView(container)
            .setPositiveButton("保存") { _, _ ->
                val host = hostInput.text.toString().trim()
                val port = portInput.text.toString().trim().toIntOrNull() ?: 1080
                val enabled = switchEnabled.isChecked
                val finalPkg = if (radioGroup.checkedRadioButtonId == radioLocal.id) selectedPackageName.get() else ""
                FirewallVpnService.setProxyConfig(host, port, enabled, finalPkg)
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
