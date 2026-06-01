package com.pocketvalo.app.ui.screen.store

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.util.Locale

// JS yang di-inject sekali saat page load untuk fix React cursor jump
private val CURSOR_FIX_JS = """
(function() {
    function fixCursor(input) {
        if (!input || input.__cursorFixed) return;
        input.__cursorFixed = true;
        var proto = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value');
        if (!proto) return;
        Object.defineProperty(input, 'value', {
            get: function() { return proto.get.call(this); },
            set: function(val) {
                var start = this.selectionStart;
                var end = this.selectionEnd;
                proto.set.call(this, val);
                if (document.activeElement === this) {
                    try { this.setSelectionRange(start, end); } catch(e) {}
                }
            },
            configurable: true
        });
    }
    document.querySelectorAll('input').forEach(fixCursor);
    var obs = new MutationObserver(function(mutations) {
        mutations.forEach(function(m) {
            m.addedNodes.forEach(function(node) {
                if (node.nodeType !== 1) return;
                if (node.tagName === 'INPUT') fixCursor(node);
                if (node.querySelectorAll) node.querySelectorAll('input').forEach(fixCursor);
            });
        });
    });
    obs.observe(document.documentElement, { childList: true, subtree: true });
})();
""".trimIndent()

class LtrWebView(context: android.content.Context) : WebView(context) {
    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val ic = super.onCreateInputConnection(outAttrs)
        outAttrs.inputType = outAttrs.inputType and
                InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS.inv() and
                InputType.TYPE_TEXT_FLAG_CAP_WORDS.inv() and
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES.inv()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            outAttrs.hintLocales = android.os.LocaleList(Locale("en", "US"))
        }
        return ic
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RiotAuthWebView(
    authUrl: String,
    onCodeReceived: (String) -> Unit,
    onDismiss: () -> Unit,
    clearSession: Boolean = true
) {
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    val enContext = remember(context) {
        val locale = Locale("en", "US")
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.createConfigurationContext(config)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
    ) {
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

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory  = { _ ->
                LtrWebView(enContext).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.textZoom          = 100
                    settings.userAgentString   =
                        "Mozilla/5.0 (Linux; Android 10; Pixel 6) AppleWebKit/537.36 " +
                                "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

                    layoutDirection = View.LAYOUT_DIRECTION_LTR
                    textDirection   = View.TEXT_DIRECTION_LTR

                    if (clearSession) {
                        CookieManager.getInstance().removeAllCookies(null)
                        CookieManager.getInstance().flush()
                        clearCache(true)
                        clearHistory()
                        WebStorage.getInstance().deleteAllData()
                    }

                    webViewClient = object : WebViewClient() {
                        private var jsInjected = false

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading  = true
                            jsInjected = false
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                            // Inject cursor fix JS sekali per page load
                            if (!jsInjected) {
                                jsInjected = true
                                view?.evaluateJavascript(CURSOR_FIX_JS, null)
                            }
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val url = request?.url?.toString() ?: return false
                            if (url.startsWith("http://localhost/redirect")) {
                                val rawCode = request.url.getQueryParameter("code")
                                val code    = rawCode?.let {
                                    java.net.URLDecoder.decode(it, "UTF-8")
                                }
                                if (code != null) onCodeReceived(code)
                                return true
                            }
                            return false
                        }
                    }

                    loadUrl(authUrl, mapOf("Accept-Language" to "en-US,en;q=0.9"))
                }
            }
        )
    }
}