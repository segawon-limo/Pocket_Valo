package com.pocketvalo.app.ui.screen.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.pocketvalo.app.data.local.AppDatabase
import com.pocketvalo.app.data.local.MultiAccountTokenStorage
import com.pocketvalo.app.data.local.TokenStorage
import com.pocketvalo.app.data.local.entity.AccountEntity
import com.pocketvalo.app.data.repository.AuthResult
import com.pocketvalo.app.data.repository.RiotAuthRepository
import com.pocketvalo.app.ui.navigation.Screen
import com.pocketvalo.app.ui.screen.store.RiotAuthWebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    navController: NavController,
    isAddAccount: Boolean = false,
    onSuccessRoute: String = Screen.Loading.route,
    popUpToRoute: String   = Screen.Login.route
) {
    val context             = LocalContext.current
    val tokenStorage        = remember { TokenStorage(context) }
    val multiStorage        = remember { MultiAccountTokenStorage(context) }
    val authRepo            = remember { RiotAuthRepository(tokenStorage, multiStorage) }
    val db                  = remember { AppDatabase.getInstance(context) }
    val previousActivePuuid = remember { if (isAddAccount) multiStorage.activePuuid else null }
    val authUrl             = remember { authRepo.generateAuthUrl() }

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
                                val accountData = result.data
                                if (isAddAccount) {
                                    launch(Dispatchers.IO) {
                                        db.accountDao().upsertAccount(
                                            AccountEntity(
                                                riotId       = "${accountData.gameName}#${accountData.tagLine}",
                                                gameName     = accountData.gameName,
                                                tagLine      = accountData.tagLine,
                                                puuid        = accountData.puuid,
                                                region       = accountData.region,
                                                accountLevel = 0,
                                                cardSmall    = null,
                                                lastSearched = System.currentTimeMillis()
                                            )
                                        )
                                    }
                                    if (previousActivePuuid != null) {
                                        multiStorage.activePuuid = previousActivePuuid
                                        tokenStorage.puuid    = previousActivePuuid
                                        tokenStorage.region   = multiStorage.getRegion(previousActivePuuid)
                                        tokenStorage.username = multiStorage.getUsername(previousActivePuuid)
                                        multiStorage.getAccessToken(previousActivePuuid)?.let { at ->
                                            tokenStorage.saveTokens(
                                                accessToken      = at,
                                                idToken          = multiStorage.getIdToken(previousActivePuuid) ?: "",
                                                refreshToken     = multiStorage.getRefreshToken(previousActivePuuid) ?: "",
                                                expiresInSeconds = ((multiStorage.getAccessExpiresAt(previousActivePuuid) - System.currentTimeMillis()) / 1000)
                                                    .toInt().coerceAtLeast(0)
                                            )
                                        }
                                        multiStorage.getEntitlementToken(previousActivePuuid)
                                            ?.let { tokenStorage.entitlementToken = it }
                                    }
                                    navController.navigate(Screen.Account.route) {
                                        popUpTo(Screen.AddAccount.route) { inclusive = true }
                                    }
                                } else {
                                    multiStorage.activePuuid = accountData.puuid
                                    launch(Dispatchers.IO) {
                                        db.accountDao().upsertAccount(
                                            AccountEntity(
                                                riotId       = "${accountData.gameName}#${accountData.tagLine}",
                                                gameName     = accountData.gameName,
                                                tagLine      = accountData.tagLine,
                                                puuid        = accountData.puuid,
                                                region       = accountData.region,
                                                accountLevel = 0,
                                                cardSmall    = null,
                                                lastSearched = System.currentTimeMillis()
                                            )
                                        )
                                    }
                                    navController.navigate(onSuccessRoute) {
                                        popUpTo(popUpToRoute) { inclusive = true }
                                    }
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
            onDismiss    = { },
            clearSession = true
        )

        if (isProcessing) {
            Box(
                modifier         = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F1923).copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFFFF4655), strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text       = if (isAddAccount) "Saving account..." else "Logging in...",
                        color      = Color.White,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (errorMsg != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .background(Color(0xFF3A0515), RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Text(text = errorMsg ?: "", color = Color(0xFFFF4655), fontSize = 13.sp)
            }
        }
    }
}