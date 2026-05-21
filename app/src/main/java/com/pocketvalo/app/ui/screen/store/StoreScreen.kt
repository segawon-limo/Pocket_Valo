package com.pocketvalo.app.ui.screen.store

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pocketvalo.app.R
import com.pocketvalo.app.ui.viewmodel.StoreUiState
import com.pocketvalo.app.ui.viewmodel.StoreViewModel
import java.util.concurrent.TimeUnit

// ── Tier config ───────────────────────────────────────────────────────────────

private data class TierConfig(
    val backgroundStart: Color,   // warna dominan (bawah/tengah)
    val backgroundEnd: Color,     // lebih gelap (atas)
    @DrawableRes val badgeRes: Int?
)

private fun tierConfigFromUuid(uuid: String?): TierConfig = when (uuid) {
    "12683d76-48d7-84a3-4e09-6985794f0445" -> TierConfig(Color(0xFF00C4C4), Color(0xFF003333), R.drawable.tier_select)
    "0cebb14f-4d31-e993-3c8d-33498af53267" -> TierConfig(Color(0xFF1A6BB5), Color(0xFF051829), R.drawable.tier_deluxe)
    "60bca084-c101-dee1-2a0e-f8f5b82dc30f" -> TierConfig(Color(0xFFD44062), Color(0xFF3A0515), R.drawable.tier_premium)
    "411e4a55-4e59-7757-41f0-86a53f101bb5" -> TierConfig(Color(0xFFD4A017), Color(0xFF2A1500), R.drawable.tier_ultra)
    "e046854e-406c-37f4-6607-19a9ba8426fc" -> TierConfig(Color(0xFFE07B20), Color(0xFF2A0E00), R.drawable.tier_exclusive)
    else -> TierConfig(Color(0xFF4A4A4A), Color(0xFF111111), null)
}

private const val VP_ICON_URL =
    "https://media.valorant-api.com/currencies/85ad13f7-3d1b-5128-9eb2-7cd8ee0b5741/displayicon.png"

// ── Root screen ───────────────────────────────────────────────────────────────

@Composable
fun StoreScreen(storeViewModel: StoreViewModel) {
    val uiState by storeViewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
    ) {
        when {
            uiState.isLoading    -> LoadingState()
            !uiState.isLoggedIn  -> LoginPrompt(onLoginClick = { storeViewModel.startLogin() })
            uiState.store != null -> StoreContent(
                uiState        = uiState,
                storeViewModel = storeViewModel,
                onRefresh      = { storeViewModel.loadStore(forceRefresh = true) },
                onLogout       = { storeViewModel.logout() }
            )
            uiState.error != null -> ErrorState(
                error   = uiState.error!!,
                onRetry = { storeViewModel.loadStore() }
            )
        }

        if (uiState.showAuthWebView && uiState.authUrl != null) {
            RiotAuthWebView(
                authUrl        = uiState.authUrl!!,
                onCodeReceived = { code -> storeViewModel.handleAuthCode(code) },
                onDismiss      = { storeViewModel.dismissAuthWebView() }
            )
        }
    }
}

// ── Store content ─────────────────────────────────────────────────────────────

@Composable
private fun StoreContent(
    uiState: StoreUiState,
    storeViewModel: StoreViewModel,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    val store = uiState.store!!
    val remainingSec = store.offersExpiresAt - System.currentTimeMillis() / 1000
    val hours   = TimeUnit.SECONDS.toHours(remainingSec)
    val minutes = TimeUnit.SECONDS.toMinutes(remainingSec) % 60

    LazyColumn(
        modifier        = Modifier.fillMaxSize(),
        contentPadding  = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text("Daily Store", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(uiState.username ?: "", color = Color(0xFF9BA3AF), fontSize = 13.sp)
                }
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF9BA3AF))
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BalanceChip(label = "VP",  amount = store.vpBalance,  color = Color(0xFF6E59F7))
                BalanceChip(label = "RAD", amount = store.radBalance, color = Color(0xFFF59E0B))
            }
        }

        item {
            Text("Resets in ${hours}h ${minutes}m", color = Color(0xFF6B7280), fontSize = 12.sp)
        }

        items(store.skinUuids) { skinUuid ->
            SkinOfferCard(
                skinUuid       = skinUuid,
                storeViewModel = storeViewModel
            )
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                Text("Logout from Store", color = Color(0xFF6B7280), fontSize = 13.sp)
            }
        }
    }
}

// ── Skin card ─────────────────────────────────────────────────────────────────

@Composable
private fun SkinOfferCard(
    skinUuid: String,
    storeViewModel: StoreViewModel
) {
    var skinName by remember { mutableStateOf("") }
    var iconUrl  by remember {
        mutableStateOf("https://media.valorant-api.com/weaponskinlevels/$skinUuid/displayicon.png")
    }
    var tierUuid  by remember { mutableStateOf<String?>(null) }
    var vpPrice   by remember { mutableStateOf<Int?>(null) }
    var videoUrl  by remember { mutableStateOf<String?>(null) }
    var showVideo by remember { mutableStateOf(false) }

    LaunchedEffect(skinUuid) {
        val info = storeViewModel.getSkinInfo(skinUuid)
        if (info != null) {
            skinName = info.displayName
            tierUuid = info.tierUuid
            videoUrl = info.videoUrl
            if (info.cost > 0) vpPrice = info.cost
            if (info.displayIcon != null) iconUrl = info.displayIcon
        }
    }

    val tier = tierConfigFromUuid(tierUuid)

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .then(
                if (videoUrl != null) Modifier.clickable { showVideo = true }
                else Modifier
            ),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    // Gradient dari atas (gelap) ke bawah (warna tier) — seperti referensi
                    Brush.verticalGradient(
                        listOf(tier.backgroundEnd, tier.backgroundStart)
                    )
                )
        ) {
            // Weapon image — tengah
            AsyncImage(
                model              = iconUrl,
                contentDescription = skinName,
                modifier           = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.78f)
                    .align(Alignment.Center)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                contentScale       = ContentScale.Fit
            )

            // Pojok kanan atas — VP price + tier badge
            Row(
                modifier              = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (vpPrice != null && vpPrice!! > 0) {
                    AsyncImage(
                        model              = VP_ICON_URL,
                        contentDescription = "VP",
                        modifier           = Modifier.size(15.dp)
                    )
                    Text(
                        text       = vpPrice.toString(),
                        color      = Color.White,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (tier.badgeRes != null) {
                    Icon(
                        painter            = painterResource(id = tier.badgeRes),
                        contentDescription = null,
                        modifier           = Modifier.size(22.dp),
                        tint               = Color.Unspecified
                    )
                }
            }

            // Bottom overlay — nama skin + tap to preview hint
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0xCC000000))
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text       = skinName.uppercase().ifEmpty { "..." },
                    color      = Color.White,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 16.sp,
                    maxLines   = 2,
                    modifier   = Modifier
                        .align(Alignment.BottomStart)
                        .padding(end = if (videoUrl != null) 100.dp else 8.dp)
                )

                // Tap to preview — hanya jika ada video
                if (videoUrl != null) {
                    Text(
                        text     = "▶  Tap to preview",
                        color    = Color(0xCCFFFFFF),
                        fontSize = 10.sp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .background(Color(0x55000000), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }

    if (showVideo && videoUrl != null) {
        SkinVideoDialog(
            skinName  = skinName,
            videoUrl  = videoUrl!!,
            onDismiss = { showVideo = false }
        )
    }
}

// ── Balance chip ──────────────────────────────────────────────────────────────

@Composable
private fun RowScope.BalanceChip(label: String, amount: Int, color: Color) {
    Row(
        modifier = Modifier
            .weight(1f)
            .background(Color(0xFF1A2332), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).background(color, RoundedCornerShape(4.dp)))
        Text(label, color = Color(0xFF9BA3AF), fontSize = 12.sp)
        Spacer(modifier = Modifier.weight(1f))
        Text(amount.toString(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

// ── States ────────────────────────────────────────────────────────────────────

@Composable
private fun LoginPrompt(onLoginClick: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🛒", fontSize = 56.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Daily Store", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Login with your Riot account to see your daily skin offers.",
            color = Color(0xFF9BA3AF), fontSize = 14.sp,
            textAlign = TextAlign.Center, lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Your password is never stored — only a secure token.",
            color = Color(0xFF6B7280), fontSize = 12.sp, textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick  = onLoginClick,
            colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4655)),
            shape    = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Login with Riot", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun LoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFFFF4655))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Loading store...", color = Color(0xFF9BA3AF), fontSize = 14.sp)
        }
    }
}

@Composable
private fun ErrorState(error: String, onRetry: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(error, color = Color(0xFF9BA3AF), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4655))) {
            Text("Retry")
        }
    }
}