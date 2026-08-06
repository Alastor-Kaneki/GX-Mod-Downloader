package dev.alastorkaneki.gxmods

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var root: FrameLayout
    private var webView: WebView? = null
    private var nativeBridge: NativeBridge? = null
    private var destroyed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyImmersiveModeSafely()

        root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(
                TextView(context).apply {
                    text = "Loading GX Store…"
                    setTextColor(Color.WHITE)
                    textSize = 16f
                    gravity = Gravity.CENTER
                },
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
        }
        setContentView(root)
        root.post { initializeWebView(resolveInitialUrl()) }
    }

    private fun resolveInitialUrl(): String = intent?.data
        ?.takeIf { it.scheme == "https" && it.host.equals("store.gx.me", ignoreCase = true) }
        ?.toString()
        ?: HOME_URL

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    private fun initializeWebView(targetUrl: String) {
        if (destroyed || isFinishing) return
        disposeWebView()

        runCatching {
            val view = WebView(this).apply {
                setBackgroundColor(Color.BLACK)
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadsImagesAutomatically = true
                    mediaPlaybackRequiresUserGesture = true
                    allowFileAccess = false
                    allowContentAccess = true
                    setSupportMultipleWindows(false)
                    javaScriptCanOpenWindowsAutomatically = false
                    userAgentString = "$userAgentString GXModDownloader/0.2.1"
                }
                webChromeClient = WebChromeClient()
            }

            val bridge = NativeBridge(this, view)
            view.addJavascriptInterface(bridge, "GXNative")
            view.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val uri = request.url
                    return if (uri.scheme.equals("https", true) || uri.scheme.equals("http", true)) {
                        false
                    } else {
                        runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                        true
                    }
                }

                override fun onPageFinished(view: WebView, url: String) {
                    runCatching { injectWrapperScripts(view, bridge) }
                        .onFailure { error ->
                            Log.e(TAG, "Script injection failed", error)
                            Toast.makeText(
                                this@MainActivity,
                                "GX downloader injection failed: ${error.message ?: error.javaClass.simpleName}",
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                }

                override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                    handler.cancel()
                }

                override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                    Log.e(TAG, "WebView renderer exited; crashed=${detail.didCrash()}")
                    root.post {
                        if (!destroyed) {
                            showStartupError(
                                title = "The Android WebView renderer stopped",
                                details = "The wrapper recovered instead of closing. Tap Retry to restart the GX Store.",
                                retryUrl = view.url ?: HOME_URL,
                            )
                        }
                    }
                    return true
                }
            }
            view.setDownloadListener { url, _, disposition, mimeType, _ ->
                bridge.downloadFromWebView(url, disposition, mimeType)
            }

            webView = view
            nativeBridge = bridge
            root.removeAllViews()
            root.addView(
                view,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                ),
            )
            view.loadUrl(targetUrl)
        }.onFailure { error ->
            Log.e(TAG, "WebView startup failed", error)
            showStartupError(
                title = "GX Mod Downloader could not start WebView",
                details = buildString {
                    append(error.javaClass.simpleName)
                    error.message?.takeIf { it.isNotBlank() }?.let { append(": ").append(it) }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val provider = WebView.getCurrentWebViewPackage()
                        append("\nWebView provider: ")
                        append(provider?.packageName ?: "none")
                        provider?.versionName?.let { append(" ").append(it) }
                    }
                },
                retryUrl = targetUrl,
            )
        }
    }

    private fun injectWrapperScripts(view: WebView, bridge: NativeBridge) {
        val themeId = JSONObject.quote(bridge.currentTheme())
        val catalog = readAsset("theme-catalog.js")
        val bootstrap = readAsset("wrapper-bootstrap.js")
        val userScript = readAsset("gx-archive-downloader.user.js")
        val combined = buildString(catalog.length + bootstrap.length + userScript.length + 256) {
            append("window.__GX_WRAPPER_THEME_ID__=").append(themeId).append(';')
            append(catalog)
            append(bootstrap)
            append("(()=>{const run=()=>{if(window.__GX_ARCHIVE_SCRIPT_INJECTED__)return;window.__GX_ARCHIVE_SCRIPT_INJECTED__=true;")
            append(userScript)
            append("};if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',run,{once:true});else run();})();")
        }
        view.evaluateJavascript(combined, null)
    }

    private fun readAsset(name: String): String = assets.open(name).bufferedReader().use { it.readText() }

    private fun showStartupError(title: String, details: String, retryUrl: String) {
        disposeWebView()
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            setBackgroundColor(Color.BLACK)

            addView(TextView(context).apply {
                text = title
                setTextColor(Color.WHITE)
                textSize = 20f
                gravity = Gravity.CENTER
            })
            addView(TextView(context).apply {
                text = details
                setTextColor(0xffc8c2d0.toInt())
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(0, 24, 0, 24)
                setTextIsSelectable(true)
            })
            addView(Button(context).apply {
                text = "Retry"
                setOnClickListener { initializeWebView(retryUrl) }
            })
            addView(Button(context).apply {
                text = "Open GX Store in browser"
                setOnClickListener {
                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(retryUrl))) }
                        .onFailure {
                            Toast.makeText(context, "No browser could open the GX Store", Toast.LENGTH_LONG).show()
                        }
                }
            })
        }
        root.removeAllViews()
        root.addView(
            panel,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        applyImmersiveModeSafely()
    }

    private fun disposeWebView() {
        val view = webView ?: return
        webView = null
        nativeBridge = null
        runCatching {
            (view.parent as? ViewGroup)?.removeView(view)
            view.stopLoading()
            view.removeJavascriptInterface("GXNative")
            view.webChromeClient = null
            view.webViewClient = WebViewClient()
            view.loadUrl("about:blank")
            view.clearHistory()
            view.removeAllViews()
            view.destroy()
        }.onFailure { Log.w(TAG, "WebView cleanup failed", it) }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        val target = resolveInitialUrl()
        val view = webView
        if (view == null) root.post { initializeWebView(target) } else view.loadUrl(target)
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()
        applyImmersiveModeSafely()
    }

    override fun onPause() {
        webView?.onPause()
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersiveModeSafely()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val view = webView
        if (view?.canGoBack() == true) view.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        destroyed = true
        disposeWebView()
        super.onDestroy()
    }

    private fun applyImmersiveModeSafely() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.setDecorFitsSystemWindows(false)
                window.insetsController?.apply {
                    hide(WindowInsets.Type.systemBars())
                    systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            }
        }.onFailure { Log.w(TAG, "Immersive mode failed", it) }
    }

    companion object {
        const val HOME_URL = "https://store.gx.me/mods/"
        private const val TAG = "GXModDownloader"
    }
}
