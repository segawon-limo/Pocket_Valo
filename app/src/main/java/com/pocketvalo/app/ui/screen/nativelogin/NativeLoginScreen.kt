package com.pocketvalo.app.ui.screen.nativelogin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun NativeLoginScreen(
    navController: NavController,
    isAddAccount: Boolean = false
) {
    val context             = LocalContext.current
    val tokenStorage        = remember { TokenStorage(context) }
    val multiStorage        = remember { MultiAccountTokenStorage(context) }
    val authRepo            = remember { RiotAuthRepository(tokenStorage, multiStorage) }
    val db                  = remember { AppDatabase.getInstance(context) }
    val previousActivePuuid = remember { if (isAddAccount) multiStorage.activePuuid else null }
    val focusManager        = LocalFocusManager.current

    var username    by remember { mutableStateOf("") }
    var password    by remember { mutableStateOf("") }
    var showPass    by remember { mutableStateOf(false) }
    var isLoading   by remember { mutableStateOf(false) }
    var errorMsg    by remember { mutableStateOf<String?>(null) }
    var showMfaHint by remember { mutableStateOf(false) }

    fun doLogin() {
        if (username.isBlank() || password.isBlank()) {
            errorMsg = "Please enter username and password"
            return
        }
        focusManager.clearFocus()
        isLoading = true
        errorMsg  = null
        showMfaHint = false

        CoroutineScope(Dispatchers.Main).launch {
            when (val result = authRepo.loginWithCredentials(username.trim(), password)) {
                is AuthResult.Success -> {
                    val accountData = result.data
                    password = "" // Clear password dari memory

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
                        }
                        navController.navigate(Screen.Account.route) {
                            popUpTo(Screen.NativeLogin.route) { inclusive = true }
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
                        navController.navigate(Screen.Loading.route) {
                            popUpTo(Screen.NativeLogin.route) { inclusive = true }
                        }
                    }
                }
                is AuthResult.Failure -> {
                    password    = ""
                    isLoading   = false
                    val msg     = result.error.message ?: "Login failed"
                    showMfaHint = msg == "MFA_REQUIRED"
                    errorMsg    = if (showMfaHint)
                        "This account has 2FA enabled.\nUse browser login instead."
                    else msg
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
    ) {
        // Background slashes
        Canvas(modifier = Modifier.fillMaxSize()) {
            val red = Color(0xFFFF4655)
            drawLine(red.copy(0.04f), Offset(-60f, size.height * 0.68f), Offset(size.width + 60f, -0.03f * size.height), 160f)
            drawLine(red.copy(0.18f), Offset(-40f, size.height * 0.26f), Offset(size.width * 0.63f, -40f), 1.5f)
            drawLine(red.copy(0.12f), Offset(20f, size.height * 0.32f), Offset(size.width + 20f, -80f), 1f)
        }

        // Top accent bar
        Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(
            Brush.horizontalGradient(listOf(Color(0xFF0F1923), Color(0xFFFF4655), Color(0xFF0F1923)))
        ))

        // Back button
        IconButton(
            onClick  = { navController.popBackStack() },
            modifier = Modifier.align(Alignment.TopStart).padding(top = 8.dp, start = 4.dp)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.width(20.dp).height(2.dp).background(Color(0xFFFF4655)))
                Text("SIGN IN", color = Color(0xFFFF4655), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }

            Text(
                text       = "Login with\nRiot Account",
                color      = Color.White,
                fontSize   = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 36.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Username
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("USERNAME", color = Color(0xFF6B7280), fontSize = 11.sp, letterSpacing = 1.sp)
                OutlinedTextField(
                    value         = username,
                    onValueChange = { username = it; errorMsg = null },
                    modifier      = Modifier.fillMaxWidth(),
                    placeholder   = { Text("Riot username", color = Color(0xFF4A5568)) },
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction    = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Color(0xFFFF4655),
                        unfocusedBorderColor = Color(0xFF2A3A4A),
                        focusedTextColor     = Color.White,
                        unfocusedTextColor   = Color.White,
                        cursorColor          = Color(0xFFFF4655)
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Password
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("PASSWORD", color = Color(0xFF6B7280), fontSize = 11.sp, letterSpacing = 1.sp)
                OutlinedTextField(
                    value         = password,
                    onValueChange = { password = it; errorMsg = null },
                    modifier      = Modifier.fillMaxWidth(),
                    placeholder   = { Text("Password", color = Color(0xFF4A5568)) },
                    singleLine    = true,
                    visualTransformation = if (showPass) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction    = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { doLogin() }),
                    trailingIcon = {
                        IconButton(onClick = { showPass = !showPass }) {
                            Icon(
                                if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = Color(0xFF6B7280)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Color(0xFFFF4655),
                        unfocusedBorderColor = Color(0xFF2A3A4A),
                        focusedTextColor     = Color.White,
                        unfocusedTextColor   = Color.White,
                        cursorColor          = Color(0xFFFF4655)
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Error
            if (errorMsg != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (showMfaHint) Color(0xFF0A2010) else Color(0xFF3A0515),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text     = errorMsg ?: "",
                        color    = if (showMfaHint) Color(0xFF4ADE80) else Color(0xFFFF4655),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Login button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isLoading) Color(0xFF9B2030) else Color(0xFFFF4655))
                    .clickable(enabled = !isLoading) { doLogin() },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                } else {
                    Text("SIGN IN", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
            }

            Text(
                text     = "Your credentials are only used to exchange for tokens\nand never stored or sent anywhere else.",
                color    = Color(0xFF4A5568),
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}