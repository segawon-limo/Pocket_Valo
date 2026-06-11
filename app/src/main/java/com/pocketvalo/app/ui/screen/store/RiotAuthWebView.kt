package com.pocketvalo.app.ui.screen.store

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.util.Locale

private val FORM_STYLE_JS = """
(function() {
    if (window.__pvStyled) return;
    window.__pvStyled = true;

    function applyStyle() {
        // Target form container — Riot login page pakai div wrapper di sekitar form
        var selectors = [
            '[class*="login-card"]',
            '[class*="card"]',
            '[class*="form"]',
            '[class*="panel"]',
            '[class*="container"]',
            'form',
            '[class*="wrapper"]'
        ];

        selectors.forEach(function(sel) {
            document.querySelectorAll(sel).forEach(function(el) {
                var rect = el.getBoundingClientRect();
                // Hanya target elemen yang cukup besar (form container, bukan wrapper kecil)
                if (rect.width > 200 && rect.height > 100) {
                    el.style.setProperty('background-color', 'rgba(10, 15, 25, 0.85)', 'important');
                    el.style.setProperty('backdrop-filter', 'blur(8px)', 'important');
                    el.style.setProperty('-webkit-backdrop-filter', 'blur(8px)', 'important');
                    el.style.setProperty('border-radius', '12px', 'important');
                    el.style.setProperty('border', '1px solid rgba(255, 70, 85, 0.2)', 'important');
                }
            });
        });

        // Input fields — pastikan text terbaca
        document.querySelectorAll('input').forEach(function(el) {
            el.style.setProperty('background-color', 'rgba(30, 40, 55, 0.9)', 'important');
            el.style.setProperty('color', '#ffffff', 'important');
            el.style.setProperty('border', '1px solid rgba(255, 255, 255, 0.15)', 'important');
        });

        // Label text
        document.querySelectorAll('label, [class*="label"]').forEach(function(el) {
            el.style.setProperty('color', 'rgba(255,255,255,0.7)', 'important');
        });
    }

    // Apply setelah page load
    applyStyle();

    // Re-apply kalau React re-render
    new MutationObserver(function() {
        applyStyle();
    }).observe(document.documentElement, { childList: true, subtree: true });
})();
""".trimIndent()

private val KEYBOARD_BRIDGE_JS = """
(function() {
    if (window.__pvBridge) return;
    window.__pvBridge = true;

    function disableNativeKeyboard(el) {
        if (!el || el.__pvDone) return;
        el.__pvDone = true;
        el.setAttribute('readonly', 'true');
        el.addEventListener('focus', function(e) {
            el.removeAttribute('readonly');
            setTimeout(function() { el.setAttribute('readonly', 'true'); }, 10);
            window.__pvActiveInput = el;
            window.__pvIsPassword  = (el.type === 'password');
            if (window.PocketValoKeyboard) {
                window.PocketValoKeyboard.onFocus(el.type === 'password' ? 'password' : 'text');
            }
        }, true);
        el.addEventListener('blur', function() {
            if (window.__pvActiveInput === el) window.__pvActiveInput = null;
        }, true);
    }

    document.querySelectorAll('input').forEach(disableNativeKeyboard);
    new MutationObserver(function(ms) {
        ms.forEach(function(m) {
            m.addedNodes.forEach(function(n) {
                if (!n || n.nodeType !== 1) return;
                if (n.tagName === 'INPUT') disableNativeKeyboard(n);
                if (n.querySelectorAll) n.querySelectorAll('input').forEach(disableNativeKeyboard);
            });
        });
    }).observe(document.documentElement, { childList: true, subtree: true });

    window.__pvInsert = function(char) {
        var el = window.__pvActiveInput;
        if (!el) return;
        el.removeAttribute('readonly');
        var start  = el.selectionStart || 0;
        var end    = el.selectionEnd   || 0;
        var newVal = el.value.slice(0, start) + char + el.value.slice(end);
        var newPos = start + char.length;
        Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set.call(el, newVal);
        el.dispatchEvent(new Event('input', { bubbles: true }));
        el.setSelectionRange(newPos, newPos);
        setTimeout(function() { el.setAttribute('readonly', 'true'); }, 10);
    };

    window.__pvBackspace = function() {
        var el = window.__pvActiveInput;
        if (!el) return;
        el.removeAttribute('readonly');
        var start = el.selectionStart || 0;
        var end   = el.selectionEnd   || 0;
        var val   = el.value;
        var newVal, newPos;
        if (start !== end) {
            newVal = val.slice(0, start) + val.slice(end); newPos = start;
        } else if (start > 0) {
            newVal = val.slice(0, start - 1) + val.slice(start); newPos = start - 1;
        } else { return; }
        Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set.call(el, newVal);
        el.dispatchEvent(new Event('input', { bubbles: true }));
        el.setSelectionRange(newPos, newPos);
        setTimeout(function() { el.setAttribute('readonly', 'true'); }, 10);
    };

    window.__pvDone = function() {
        var el = window.__pvActiveInput;
        if (el) el.blur();
        var btn = document.querySelector('button[type=submit]') ||
                  document.querySelector('[data-testid*="submit"]') ||
                  document.querySelector('button');
        if (btn) btn.click();
    };
})();
""".trimIndent()

// ── WebView subclass ──────────────────────────────────────────────────────────
// Tiga override diperlukan untuk benar-benar suppress IME di semua device termasuk MIUI:
// 1. onCreateInputConnection → null   : tidak ada InputConnection = IME tidak bisa attach
// 2. onCheckIsTextEditor → false       : sistem tidak anggap ini text editor
// 3. isFocusableInTouchMode override  : cegah WebView request focus yang trigger IME
class PocketValoWebView(context: Context) : WebView(context) {

    // Tidak return InputConnection — system tidak bisa attach IME sama sekali
    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? = null
    override fun onCheckIsTextEditor(): Boolean = false

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        hideImeNow()
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus) hideImeNow()
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: android.graphics.Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        // Intercept tepat saat WebView dapat focus — sebelum system decide show IME
        if (gainFocus) hideImeNow()
    }

    override fun onTouchEvent(event: android.view.MotionEvent?): Boolean {
        val result = super.onTouchEvent(event)
        hideImeNow()
        return result
    }

    fun hideImeNow() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            ?: return
        // Method 1: standard hide
        imm.hideSoftInputFromWindow(windowToken, 0)
        // Method 2: hide dari sisi input method itu sendiri (lebih aggressive)
        @Suppress("DEPRECATION")
        imm.hideSoftInputFromWindow(applicationWindowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        // Method 3: WindowInsetsController (Android 11+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            (context as? android.app.Activity)?.window?.let { win ->
                androidx.core.view.WindowInsetsControllerCompat(win, this)
                    .hide(androidx.core.view.WindowInsetsCompat.Type.ime())
            }
        }
        // Method 4: temporarily set FLAG_ALT_FOCUSABLE_IM di window
        // Ini prevent window dari menerima IME focus sama sekali
        (context as? android.app.Activity)?.window?.let { win ->
            win.addFlags(android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            postDelayed({
                win.clearFlags(android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            }, 200)
        }
    }
}

class KeyboardBridge(
    private val onFieldFocused: (isPassword: Boolean) -> Unit
) {
    @android.webkit.JavascriptInterface
    fun onFocus(type: String) {
        onFieldFocused(type == "password")
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
    var isLoading       by remember { mutableStateOf(true) }
    var showKeyboard    by remember { mutableStateOf(false) }
    var isPasswordField by remember { mutableStateOf(false) }
    var webViewRef      by remember { mutableStateOf<WebView?>(null) }

    val context   = LocalContext.current
    val activity  = context as? com.pocketvalo.app.MainActivity
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // Aktifkan IME suppression saat composable ini hidup
    DisposableEffect(lifecycle) {
        activity?.suppressIme = true
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> activity?.suppressIme = true
                Lifecycle.Event.ON_PAUSE  -> activity?.suppressIme = false
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            activity?.suppressIme = false
            lifecycle.removeObserver(observer)
        }
    }

    val enContext = remember(context) {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale("en", "US"))
        context.createConfigurationContext(config)
    }

    val inject: (String) -> Unit = { js -> webViewRef?.post { webViewRef?.evaluateJavascript(js, null) } }

    // Intercept back button — dismiss keyboard dulu sebelum keluar dari screen
    BackHandler(enabled = showKeyboard) {
        showKeyboard = false
        // Blur input aktif supaya tap berikutnya trigger focus event lagi
        inject("if (window.__pvActiveInput) { window.__pvActiveInput.blur(); window.__pvActiveInput = null; }")
    }

    fun forceHideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        webViewRef?.let { imm?.hideSoftInputFromWindow(it.windowToken, 0) }
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
                Spacer(Modifier.width(12.dp))
            }
        }

        AndroidView(
            modifier = Modifier.fillMaxWidth().weight(1f),
            factory  = { _ ->
                PocketValoWebView(enContext).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.textZoom          = 100
                    settings.userAgentString   =
                        "Mozilla/5.0 (Linux; Android 10; Pixel 6) AppleWebKit/537.36 " +
                                "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

                    layoutDirection = View.LAYOUT_DIRECTION_LTR
                    textDirection   = View.TEXT_DIRECTION_LTR

                    addJavascriptInterface(
                        KeyboardBridge { isPassword ->
                            isPasswordField = isPassword
                            showKeyboard    = true
                            forceHideKeyboard()
                        },
                        "PocketValoKeyboard"
                    )

                    if (clearSession) {
                        CookieManager.getInstance().removeAllCookies(null)
                        CookieManager.getInstance().flush()
                        clearCache(true)
                        clearHistory()
                        WebStorage.getInstance().deleteAllData()
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading    = true
                            showKeyboard = false
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                            view?.evaluateJavascript(KEYBOARD_BRIDGE_JS, null)
                            view?.evaluateJavascript(FORM_STYLE_JS, null)
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val url = request?.url?.toString() ?: return false
                            if (url.startsWith("http://localhost/redirect")) {
                                val code = request.url.getQueryParameter("code")
                                    ?.let { java.net.URLDecoder.decode(it, "UTF-8") }
                                if (code != null) onCodeReceived(code)
                                return true
                            }
                            return false
                        }
                    }

                    webViewRef = this
                    loadUrl(authUrl, mapOf("Accept-Language" to "en-US,en;q=0.9"))
                }
            }
        )

        AnimatedVisibility(
            visible = showKeyboard,
            enter   = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit    = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            PocketKeyboard(
                isPasswordField = isPasswordField,
                onChar      = { char -> inject("window.__pvInsert(${org.json.JSONObject.quote(char)});") },
                onBackspace = { inject("window.__pvBackspace();") },
                onDone      = {
                    if (isPasswordField) {
                        inject("window.__pvDone();")
                        showKeyboard = false
                    } else {
                        inject("""
                            (function(){
                                var inputs = document.querySelectorAll('input');
                                for(var i=0;i<inputs.length-1;i++){
                                    if(inputs[i]===window.__pvActiveInput){
                                        inputs[i+1].removeAttribute('readonly');
                                        inputs[i+1].focus();
                                        inputs[i+1].click();
                                        break;
                                    }
                                }
                            })();
                        """.trimIndent())
                    }
                }
            )
        }
    }
}