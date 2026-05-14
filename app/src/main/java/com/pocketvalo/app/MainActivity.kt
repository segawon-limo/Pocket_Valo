package com.pocketvalo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pocketvalo.app.ui.navigation.AppNavigation
import com.pocketvalo.app.ui.theme.PocketValoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PocketValoTheme {
                AppNavigation()
            }
        }
    }
}