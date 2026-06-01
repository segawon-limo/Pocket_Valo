package com.pocketvalo.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import com.pocketvalo.app.data.repository.AuthCodeHolder
import com.pocketvalo.app.ui.navigation.AppNavigation
import com.pocketvalo.app.ui.theme.PocketValoTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        // Handle redirect yang datang saat app sedang tidak aktif
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

    // Handle redirect dari Chrome Custom Tab (pocketvalo://auth?code=xxx)
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