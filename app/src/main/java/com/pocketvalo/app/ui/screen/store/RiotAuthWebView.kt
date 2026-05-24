package com.pocketvalo.app.ui.screen.store

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RiotAuthWebView(
    authUrl: String,
    onCodeReceived: (String) -> Unit,
    onDismiss: () -> Unit,
    clearSession: Boolean = false   // true saat add-account, false saat login biasa
) {
    var isLoading by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A2332))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
            Text(
                text     = "Login with Riot",
                color    = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            if (isLoading) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(20.dp),
                    color       = Color(0xFFFF4655),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
        }

        // ── WebView ───────────────────────────────────────────────────────────
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory  = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString   =
                        "Mozilla/5.0 (Linux; Android 10; Pixel 6) AppleWebKit/537.36 " +
                                "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

                    // Clear cookies dan storage saat add-account
                    // supaya Riot tidak auto-login dengan akun yang sudah masuk
                    if (clearSession) {
                        CookieManager.getInstance().removeAllCookies(null)
                        CookieManager.getInstance().flush()
                        clearCache(true)
                        clearHistory()
                        WebStorage.getInstance().deleteAllData()
                        android.util.Log.d("RiotAuthWebView", "Session cleared for add-account flow")
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val url = request?.url?.toString() ?: return false
                            android.util.Log.d("RiotAuthWebView", "URL: $url")

                            if (url.startsWith("http://localhost/redirect")) {
                                val rawCode = request.url.getQueryParameter("code")
                                val code    = rawCode?.let {
                                    java.net.URLDecoder.decode(it, "UTF-8")
                                }
                                android.util.Log.d("RiotAuthWebView", "Code received: ${code?.take(10)}...")
                                if (code != null) onCodeReceived(code)
                                return true
                            }
                            return false
                        }
                    }

                    loadUrl(authUrl)
                }
            }
        )
    }
}