package com.snowsniw.lowcord

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.view.View
import android.webkit.*
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var pendingPermissionRequest: PermissionRequest? = null

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceBackgroundService()
            pendingPermissionRequest?.let { it.grant(it.resources) }
        } else {
            pendingPermissionRequest?.deny()
            Toast.makeText(this, "Без доступа к микрофону голосовой чат не работает", Toast.LENGTH_SHORT).show()
        }
        pendingPermissionRequest = null
    }

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        filePathCallback?.onReceiveValue(data)
        filePathCallback = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)

        applySystemBarsTheme()
        applyProxySettings(showToast = false)
        setupCookies()
        setupWebView()
        setupDownloads()
        setupBackNavigation()

        if (savedInstanceState == null) {
            webView.loadUrl("https://discord.com/login")
        }
    }

    private fun applySystemBarsTheme() {
        val bgColor = if (PrefsManager.isAmoledMode(this)) Color.BLACK else Color.parseColor("#1E1F22")
        (webView.parent as? View)?.setBackgroundColor(bgColor)
        @Suppress("DEPRECATION")
        window.statusBarColor = bgColor
        @Suppress("DEPRECATION")
        window.navigationBarColor = bgColor
    }

    private fun applyProxySettings(showToast: Boolean) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            if (showToast && PrefsManager.isProxyEnabled(this)) {
                Toast.makeText(this, "Прокси не поддерживается версией WebView", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        if (PrefsManager.isProxyEnabled(this)) {
            val proxyUrl = PrefsManager.getProxyUrl(this)
            try {
                val config = ProxyConfig.Builder()
                    .addProxyRule(proxyUrl)
                    .build()
                ProxyController.getInstance().setProxyOverride(config, executor) {
                    if (showToast) {
                        Toast.makeText(this, "Прокси применён: $proxyUrl", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                if (showToast) {
                    Toast.makeText(this, "Неверный формат прокси (пример: socks5://127.0.0.1:1080)", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            ProxyController.getInstance().clearProxyOverride(executor) {
                if (showToast) {
                    Toast.makeText(this, "Прокси отключён", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showSettingsDialog() {
        val dp = resources.displayMetrics.density
        val pad = (20 * dp).toInt()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, (10 * dp).toInt())
            setBackgroundColor(Color.parseColor("#18191C"))
        }

        val title = TextView(this).apply {
            text = "Настройки Lowcord"
            textSize = 20f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, (16 * dp).toInt())
        }
        layout.addView(title)

        val perfSwitch = SwitchCompat(this).apply {
            text = "Режим без лагов (откл. анимации и размытие)"
            setTextColor(Color.parseColor("#E0E1E5"))
            isChecked = PrefsManager.isPerfMode(this@MainActivity)
            setPadding(0, (8 * dp).toInt(), 0, (8 * dp).toInt())
        }
        layout.addView(perfSwitch)

        val amoledSwitch = SwitchCompat(this).apply {
            text = "Чисто чёрная AMOLED-тема"
            setTextColor(Color.parseColor("#E0E1E5"))
            isChecked = PrefsManager.isAmoledMode(this@MainActivity)
            setPadding(0, (8 * dp).toInt(), 0, (12 * dp).toInt())
        }
        layout.addView(amoledSwitch)

        val proxySwitch = SwitchCompat(this).apply {
            text = "Встроенный прокси (только для Lowcord)"
            setTextColor(Color.parseColor("#E0E1E5"))
            isChecked = PrefsManager.isProxyEnabled(this@MainActivity)
            setPadding(0, (8 * dp).toInt(), 0, (8 * dp).toInt())
        }
        layout.addView(proxySwitch)

        val proxyInput = EditText(this).apply {
            hint = "socks5://127.0.0.1:1080 или http://host:port"
            setHintTextColor(Color.parseColor("#80848E"))
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#2B2D31"))
            setPadding((12 * dp).toInt(), (10 * dp).toInt(), (12 * dp).toInt(), (10 * dp).toInt())
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setText(PrefsManager.getProxyUrl(this@MainActivity))
            visibility = if (proxySwitch.isChecked) View.VISIBLE else View.GONE
        }
        layout.addView(proxyInput)

        proxySwitch.setOnCheckedChangeListener { _, isChecked ->
            proxyInput.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        val dialog = AlertDialog.Builder(this)
            .setView(layout)
            .setPositiveButton("Сохранить") { _, _ ->
                PrefsManager.setPerfMode(this, perfSwitch.isChecked)
                PrefsManager.setAmoledMode(this, amoledSwitch.isChecked)
                PrefsManager.setProxyEnabled(this, proxySwitch.isChecked)
                PrefsManager.setProxyUrl(this, proxyInput.text.toString())

                applySystemBarsTheme()
                WebTweaks.applyThemeAndPerf(webView, perfSwitch.isChecked, amoledSwitch.isChecked)
                applyProxySettings(showToast = true)
            }
            .setNeutralButton("Очистить кэш") { _, _ ->
                webView.clearCache(true)
                webView.reload()
                Toast.makeText(this, "Кэш очищен", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .create()

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.background_dark)
    }

    private fun setupCookies() {
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        }

        webView.addJavascriptInterface(LowcordBridge(), "LowcordBridge")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url ?: return false
                val host = url.host ?: return false

                return if (host.endsWith("discord.com") || host.endsWith("discordapp.com") || host.endsWith("discord.gg")) {
                    false
                } else {
                    startActivity(Intent(Intent.ACTION_VIEW, url))
                    true
                }
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                WebTweaks.spoofDesktopBrowser(view)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                WebTweaks.applyAll(
                    view,
                    PrefsManager.isPerfMode(this@MainActivity),
                    PrefsManager.isAmoledMode(this@MainActivity)
                )
            }

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                super.doUpdateVisitedHistory(view, url, isReload)
                WebTweaks.applyAll(
                    view,
                    PrefsManager.isPerfMode(this@MainActivity),
                    PrefsManager.isAmoledMode(this@MainActivity)
                )
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread {
                    if (request.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                        val hasMic = ContextCompat.checkSelfPermission(
                            this@MainActivity, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasMic) {
                            startVoiceBackgroundService()
                            request.grant(request.resources)
                        } else {
                            pendingPermissionRequest = request
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    } else {
                        request.deny()
                    }
                }
            }

            override fun onShowFileChooser(
                webView: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = callback

                val intent = params?.createIntent() ?: return false
                return try {
                    fileChooserLauncher.launch(intent)
                    true
                } catch (e: Exception) {
                    filePathCallback = null
                    false
                }
            }
        }
    }

    private fun startVoiceBackgroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotif = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasNotif) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        VoiceService.start(this)
    }

    private inner class LowcordBridge {
        @JavascriptInterface
        fun onVoiceStarted() {
            runOnUiThread {
                val hasMic = ContextCompat.checkSelfPermission(
                    this@MainActivity, Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (hasMic) {
                    startVoiceBackgroundService()
                }
            }
        }

        @JavascriptInterface
        fun onVoiceStopped() {
            runOnUiThread {
                VoiceService.stop(this@MainActivity)
            }
        }

        @JavascriptInterface
        fun openSettings() {
            runOnUiThread {
                showSettingsDialog()
            }
        }
    }

    private fun setupDownloads() {
        webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            try {
                val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                val request = DownloadManager.Request(Uri.parse(url)).apply {
                    setMimeType(mimeType)
                    addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url))
                    addRequestHeader("User-Agent", userAgent)
                    setTitle(fileName)
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                }
                val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)
                Toast.makeText(this, "Скачивание: $fileName", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }

    override fun onDestroy() {
        if (isFinishing) {
            VoiceService.stop(this)
        }
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        webView.restoreState(savedInstanceState)
    }
}
