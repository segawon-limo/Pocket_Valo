package com.pocketvalo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pocketvalo.app.ui.navigation.AppNavigation
import com.pocketvalo.app.ui.theme.PocketValoTheme
import com.pocketvalo.app.ui.viewmodel.StoreViewModel

class MainActivity : ComponentActivity() {

    private val storeViewModel: StoreViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PocketValoTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F1923))
                ) {
                    AppNavigation(storeViewModel = storeViewModel)
                }
            }
        }
    }
}