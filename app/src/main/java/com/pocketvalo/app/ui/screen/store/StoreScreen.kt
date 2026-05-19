package com.pocketvalo.app.ui.screen.store

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.pocketvalo.app.ui.viewmodel.StoreUiState
import com.pocketvalo.app.ui.viewmodel.StoreViewModel
import java.util.concurrent.TimeUnit

@Composable
fun StoreScreen(
    storeViewModel: StoreViewModel = viewModel()
) {
    val uiState by storeViewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F1923))
        ) {
            when {
                uiState.isLoading -> LoadingState()
                !uiState.isLoggedIn -> LoginPrompt(
                    onLoginClick = { storeViewModel.startLogin() }
                )
                uiState.store != null -> StoreContent(
                    uiState = uiState,
                    storeViewModel = storeViewModel,
                    onRefresh = { storeViewModel.loadStore(forceRefresh = true) },
                    onLogout = { storeViewModel.logout() }
                )
                uiState.error != null -> ErrorState(
                    error = uiState.error!!,
                    onRetry = { storeViewModel.loadStore() }
                )
            }
        }

        if (uiState.showAuthWebView && uiState.authUrl != null) {
            RiotAuthWebView(
                authUrl = uiState.authUrl!!,
                onCodeReceived = { code -> storeViewModel.handleAuthCode(code) },
                onDismiss = { storeViewModel.dismissAuthWebView() }
            )
        }
    }
}

@Composable
private fun LoginPrompt(onLoginClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🛒", fontSize = 56.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Daily Store",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Login with your Riot account to see your daily skin offers.",
            color = Color(0xFF9BA3AF),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Your password is never stored — only a secure token.",
            color = Color(0xFF6B7280),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onLoginClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4655)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "Login with Riot",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun StoreContent(
    uiState: StoreUiState,
    storeViewModel: StoreViewModel,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    val store = uiState.store!!
    val remainingSec = store.offersExpiresAt - System.currentTimeMillis() / 1000
    val hours = TimeUnit.SECONDS.toHours(remainingSec)
    val minutes = TimeUnit.SECONDS.toMinutes(remainingSec) % 60

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Daily Store",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = uiState.username ?: "",
                        color = Color(0xFF9BA3AF),
                        fontSize = 13.sp
                    )
                }
                IconButton(onClick = onRefresh) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color(0xFF9BA3AF)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BalanceChip(label = "VP", amount = store.vpBalance, color = Color(0xFF6E59F7))
                BalanceChip(label = "RAD", amount = store.radBalance, color = Color(0xFFF59E0B))
            }
        }

        item {
            Text(
                text = "Resets in ${hours}h ${minutes}m",
                color = Color(0xFF6B7280),
                fontSize = 12.sp
            )
        }

        items(store.skinUuids) { skinUuid ->
            SkinOfferCard(skinUuid = skinUuid, storeViewModel = storeViewModel)
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Logout from Store", color = Color(0xFF6B7280), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SkinOfferCard(skinUuid: String, storeViewModel: StoreViewModel) {
    var skinName by remember { mutableStateOf("") }
    var iconUrl by remember {
        mutableStateOf("https://media.valorant-api.com/weaponskinlevels/$skinUuid/displayicon.png")
    }

    LaunchedEffect(skinUuid) {
        val info = storeViewModel.getSkinInfo(skinUuid)
        if (info != null) {
            skinName = info.displayName
            if (info.displayIcon != null) iconUrl = info.displayIcon
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2332))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = iconUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = skinName.ifEmpty { skinUuid.take(8) + "..." },
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun RowScope.BalanceChip(label: String, amount: Int, color: Color) {
    Row(
        modifier = Modifier
            .weight(1f)
            .background(Color(0xFF1A2332), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(4.dp))
        )
        Text(text = label, color = Color(0xFF9BA3AF), fontSize = 12.sp)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = amount.toString(),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFFFF4655))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Logging in...", color = Color(0xFF9BA3AF), fontSize = 14.sp)
        }
    }
}

@Composable
private fun ErrorState(error: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(error, color = Color(0xFF9BA3AF), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4655))
        ) {
            Text("Retry")
        }
    }
}