package com.pocketvalo.app.ui.screen.welcome

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.pocketvalo.app.data.local.AppDatabase
import com.pocketvalo.app.data.local.MultiAccountTokenStorage
import com.pocketvalo.app.data.local.TokenStorage
import com.pocketvalo.app.data.local.entity.AccountEntity
import com.pocketvalo.app.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(navController: NavController) {
    val context      = LocalContext.current
    val tokenStorage = remember { TokenStorage(context) }
    val multiStorage = remember { MultiAccountTokenStorage(context) }
    val db           = remember { AppDatabase.getInstance(context) }

    var savedAccounts    by remember { mutableStateOf<List<AccountEntity>>(emptyList()) }
    var showAccountSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        db.accountDao().getAllAccounts().collect { savedAccounts = it }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
    ) {
        // ── Diagonal slash background ─────────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val red = Color(0xFFFF4655)
            val w = size.width
            val h = size.height

            // Big soft diagonal slash
            drawLine(
                color       = red.copy(alpha = 0.04f),
                start       = Offset(-60f, h * 0.68f),
                end         = Offset(w + 60f, -0.03f * h),
                strokeWidth = 160f
            )
            // Fine accent lines top-left
            drawLine(red.copy(0.18f), Offset(-40f, h * 0.26f), Offset(w * 0.63f, -40f), 1.5f)
            drawLine(red.copy(0.12f), Offset(20f,  h * 0.32f), Offset(w + 20f,   -80f), 1f)
            drawLine(red.copy(0.08f), Offset(-20f, h * 0.42f), Offset(w * 0.88f, -20f), 0.8f)
            // Fine accent lines bottom-right
            drawLine(red.copy(0.15f), Offset(w * 0.5f,  h + 40f), Offset(w + 60f, h * 0.55f), 1.5f)
            drawLine(red.copy(0.10f), Offset(w * 0.25f, h + 40f), Offset(w + 40f, h * 0.48f), 1f)
        }

        // Top accent bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF0F1923), Color(0xFFFF4655), Color(0xFF0F1923))
                    )
                )
        )

        // ── Content ───────────────────────────────────────────────────────────
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top section — label + app name + tagline
            Column(modifier = Modifier.padding(top = 72.dp)) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.width(20.dp).height(2.dp).background(Color(0xFFFF4655)))
                    Text(
                        text          = "VALORANT COMPANION",
                        color         = Color(0xFFFF4655),
                        fontSize      = 11.sp,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text          = "POCKET",
                    color         = Color.White,
                    fontSize      = 52.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    lineHeight    = 52.sp,
                    letterSpacing = (-1).sp
                )
                Text(
                    text          = "VALO",
                    color         = Color(0xFFFF4655),
                    fontSize      = 52.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    lineHeight    = 52.sp,
                    letterSpacing = (-1).sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text       = "Track your stats, store, and\nmatch history — all in one place.",
                    color      = Color(0xFF6B7280),
                    fontSize   = 14.sp,
                    lineHeight = 22.sp
                )
            }

            // Bottom section — button
            Column(modifier = Modifier.padding(bottom = 56.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier              = Modifier.padding(bottom = 28.dp)
                ) {
                    Box(modifier = Modifier.size(6.dp).background(Color(0xFFFF4655), RoundedCornerShape(50)))
                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF1A2332), RoundedCornerShape(50)))
                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF1A2332), RoundedCornerShape(50)))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFF4655))
                        .clickable {
                            when {
                                savedAccounts.size > 1 -> showAccountSheet = true
                                tokenStorage.isLoggedIn -> navController.navigate(Screen.Loading.route) {
                                    popUpTo(Screen.Welcome.route) { inclusive = true }
                                }
                                else -> navController.navigate(Screen.Login.route) {
                                    popUpTo(Screen.Welcome.route) { inclusive = true }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text          = "LOGIN",
                        color         = Color.White,
                        fontSize      = 15.sp,
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text      = "Sign in with your Riot account",
                    color     = Color(0xFF3A4A5C),
                    fontSize  = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tombol alternatif: native login (username/password langsung)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1A2332))
                        .clickable {
                            navController.navigate(com.pocketvalo.app.ui.navigation.Screen.NativeLogin.route)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text          = "Sign in with username & password",
                        color         = Color(0xFF9BA3AF),
                        fontSize      = 13.sp,
                        fontWeight    = FontWeight.Medium
                    )
                }

                Text(
                    text      = "Alternative login (experimental)",
                    color     = Color(0xFF3A4A5C),
                    fontSize  = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // ── Account picker bottom sheet ───────────────────────────────────────────
    if (showAccountSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAccountSheet = false },
            containerColor   = Color(0xFF1A2332),
            scrimColor       = Color.Black.copy(alpha = 0.6f),
            shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            AccountPickerSheet(
                accounts     = savedAccounts,
                activePuuid  = multiStorage.activePuuid,
                onPick       = { account ->
                    showAccountSheet = false
                    // Set active account
                    multiStorage.activePuuid = account.puuid
                    tokenStorage.puuid    = account.puuid
                    tokenStorage.region   = multiStorage.getRegion(account.puuid)
                    tokenStorage.username = multiStorage.getUsername(account.puuid)
                    multiStorage.getAccessToken(account.puuid)?.let { at ->
                        tokenStorage.saveTokens(
                            accessToken      = at,
                            idToken          = multiStorage.getIdToken(account.puuid) ?: "",
                            refreshToken     = multiStorage.getRefreshToken(account.puuid) ?: "",
                            expiresInSeconds = ((multiStorage.getAccessExpiresAt(account.puuid) - System.currentTimeMillis()) / 1000)
                                .toInt().coerceAtLeast(0)
                        )
                    }
                    multiStorage.getEntitlementToken(account.puuid)
                        ?.let { tokenStorage.entitlementToken = it }

                    navController.navigate(Screen.Loading.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onAddAccount = {
                    showAccountSheet = false
                    navController.navigate(Screen.AddAccount.route)
                }
            )
        }
    }
}

// ── Account picker sheet content ──────────────────────────────────────────────

@Composable
private fun AccountPickerSheet(
    accounts: List<AccountEntity>,
    activePuuid: String?,
    onPick: (AccountEntity) -> Unit,
    onAddAccount: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 36.dp)
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF3A4A5C))
                .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text          = "CHOOSE ACCOUNT",
            color         = Color(0xFF9BA3AF),
            fontSize      = 11.sp,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(14.dp))

        accounts.forEach { account ->
            val isLast = account.puuid == activePuuid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isLast) Color(0xFF1E3A2E) else Color(0xFF0F1923))
                    .clickable { onPick(account) }
                    .padding(12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1A2332))
                ) {
                    if (account.cardSmall != null) {
                        AsyncImage(
                            model              = account.cardSmall,
                            contentDescription = null,
                            modifier           = Modifier.fillMaxSize()
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = account.riotId,
                        color      = if (isLast) Color(0xFF4ADE80) else Color.White,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Text(
                        text     = account.region.uppercase(),
                        color    = Color(0xFF6B7280),
                        fontSize = 11.sp
                    )
                }

                if (isLast) {
                    Text(
                        text     = "Last used",
                        color    = Color(0xFF4ADE80),
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onAddAccount() }
                .background(Color(0xFF0F1923))
                .padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier         = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A2332)),
                contentAlignment = Alignment.Center
            ) {
                Text("+", color = Color(0xFFFF4655), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Text("Add Account", color = Color(0xFFFF4655), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}