package com.pocketvalo.app.ui.screen.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pocketvalo.app.data.local.TokenStorage
import com.pocketvalo.app.data.repository.AuthResult
import com.pocketvalo.app.data.repository.RiotAuthRepository
import com.pocketvalo.app.ui.navigation.Screen
import com.pocketvalo.app.ui.screen.store.RiotAuthWebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val tokenStorage = remember { TokenStorage(context) }
    val authRepo     = remember { RiotAuthRepository(tokenStorage) }
    val authUrl      = remember { authRepo.generateAuthUrl() }

    var isProcessing by remember { mutableStateOf(false) }
    var errorMsg     by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
    ) {
        RiotAuthWebView(
            authUrl        = authUrl,
            onCodeReceived = { code ->
                if (!isProcessing) {
                    isProcessing = true
                    CoroutineScope(Dispatchers.Main).launch {
                        when (val result = authRepo.loginWithCode(code)) {
                            is AuthResult.Success -> {
                                navController.navigate(Screen.Loading.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            }
                            is AuthResult.Failure -> {
                                errorMsg     = result.error.message ?: "Login failed"
                                isProcessing = false
                            }
                        }
                    }
                }
            },
            onDismiss = {
                // User closed WebView — stay on LoginScreen (they can retry)
            }
        )

        // Processing overlay
        if (isProcessing) {
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F1923).copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color       = Color(0xFFFF4655),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text      = "Logging in...",
                        color     = Color.White,
                        fontSize  = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Error snackbar
        if (errorMsg != null) {
            Box(
                modifier         = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .background(Color(0xFF3A0515), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text     = errorMsg ?: "",
                    color    = Color(0xFFFF4655),
                    fontSize = 13.sp
                )
            }
        }
    }
}