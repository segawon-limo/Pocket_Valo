package com.pocketvalo.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.pocketvalo.app.data.repository.AuthCodeHolder
import com.pocketvalo.app.ui.navigation.AppNavigation
import com.pocketvalo.app.ui.theme.PocketValoTheme

class MainActivity : ComponentActivity() {

    var suppressIme: Boolean = false
    private val handler = Handler(Looper.getMainLooper())
    private val hideImeRunnable = Runnable { forceHideIme() }

    private fun forceHideIme() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsControllerCompat(window, window.decorView)
                .hide(WindowInsetsCompat.Type.ime())
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (suppressIme) {
            when (ev?.action) {
                MotionEvent.ACTION_DOWN -> {
                    forceHideIme()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(hideImeRunnable)
                    handler.post(hideImeRunnable)           // immediate
                    handler.postDelayed(hideImeRunnable, 50)
                    handler.postDelayed(hideImeRunnable, 150)
                    handler.postDelayed(hideImeRunnable, 300)
                    handler.postDelayed(hideImeRunnable, 500)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onDestroy() {
        handler.removeCallbacks(hideImeRunnable)
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN or
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        )

        handleAuthIntent(intent)

        setContent {
            PocketValoTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F1923))
                ) {
                    AppNavigation()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAuthIntent(intent)
    }

    private fun handleAuthIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "pocketvalo" && data.host == "auth") {
            val code = data.getQueryParameter("code") ?: return
            val decoded = java.net.URLDecoder.decode(code, "UTF-8")
            android.util.Log.d("MainActivity", "Auth code received: ${decoded.take(10)}...")
            AuthCodeHolder.emit(decoded)
        }
    }
}