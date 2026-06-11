package com.pocketvalo.app.ui.screen.store

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.pocketvalo.app.data.model.BundleDetail
import com.pocketvalo.app.data.repository.BundleInfo
import com.pocketvalo.app.ui.viewmodel.StoreUiState
import com.pocketvalo.app.ui.viewmodel.StoreViewModel
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.Image

// VP_ICON_URL, TierConfig, tierConfigFromUuid — defined in TierConfig.kt (same package)
private const val RP_ICON_URL = "https://media.valorant-api.com/currencies/e59aa87c-4cbf-517a-5983-6e81511be9b7/displayicon.png"

// ── Root screen ───────────────────────────────────────────────────────────────

@Composable
fun StoreScreen(
    storeViewModel: StoreViewModel,
    navController: androidx.navigation.NavController? = null
) {
    val uiState by storeViewModel.uiState.collectAsState()

    // Reset sessionExpired yang mungkin ter-set sebelum login selesai
    // LaunchedEffect(Unit) dipanggil sekali saat StoreScreen pertama di-compose
    // Ini memastikan state stale tidak langsung trigger redirect ke login
    LaunchedEffect(Unit) {
        if (uiState.sessionExpired) {
            storeViewModel.resetSessionExpired()
            storeViewModel.loadStore(forceRefresh = true)
        }
    }

    LaunchedEffect(uiState.sessionExpired) {
        if (uiState.sessionExpired) {
            navController?.navigate(com.pocketvalo.app.ui.navigation.Screen.Login.route) {
                popUpTo(com.pocketvalo.app.ui.navigation.Screen.Home.route) { inclusive = false }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
    ) {
        when {
            uiState.isLoading     -> LoadingState()
            uiState.store != null -> StoreContent(
                uiState        = uiState,
                storeViewModel = storeViewModel,
                onRefresh      = { storeViewModel.loadStore(forceRefresh = true) },
                onNightMarket  = { navController?.navigate(com.pocketvalo.app.ui.navigation.Screen.NightMarket.route) },
                onWatchlist    = { navController?.navigate(com.pocketvalo.app.ui.navigation.Screen.Watchlist.route) },
                onBundleClick  = { uuid, duration ->
                    android.util.Log.d("StoreScreen", "Bundle clicked: $uuid duration=$duration")
                    val bundle    = uiState.store?.bundles?.firstOrNull { it.uuid == uuid }
                    val basePrice = bundle?.totalBasePrice ?: 0
                    val discPrice = bundle?.totalDiscountedPrice ?: 0
                    val route = com.pocketvalo.app.ui.navigation.Screen.BundleDetail
                        .createRoute(uuid, duration, basePrice, discPrice)
                    android.util.Log.d("StoreScreen", "Navigating to: $route")
                    navController?.navigate(route)
                }
            )
            uiState.error != null -> ErrorState(
                error   = uiState.error!!,
                onRetry = { storeViewModel.loadStore() }
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
    onNightMarket: () -> Unit,
    onWatchlist: () -> Unit,
    onBundleClick: (uuid: String, duration: Long) -> Unit
) {
    val store = uiState.store!!
    val remainingSec = store.offersExpiresAt - System.currentTimeMillis() / 1000
    val hours   = TimeUnit.SECONDS.toHours(remainingSec)
    val minutes = TimeUnit.SECONDS.toMinutes(remainingSec) % 60
    val hasNightMarket = store.nightMarketOffers.isNotEmpty()

    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
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
                Row {
                    IconButton(onClick = onWatchlist) {
                        Icon(
                            imageVector        = Icons.Default.ShoppingCart,
                            contentDescription = "Watchlist",
                            tint               = Color(0xFF9BA3AF)
                        )
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF9BA3AF))
                    }
                }
            }
        }

        // ── Balance row ───────────────────────────────────────────────────────
        item {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                BalanceChip(modifier = Modifier.weight(1f), label = "VP", iconUrl = VP_ICON_URL, amount = store.vpBalance)
                BalanceChip(modifier = Modifier.weight(1f), label = "RP", iconUrl = RP_ICON_URL, amount = store.radBalance)
                if (hasNightMarket) {
                    NightMarketChip(onClick = onNightMarket)
                }
            }
        }

        // ── Reset timer ───────────────────────────────────────────────────────
        item {
            Text("Resets in ${hours}h ${minutes}m", color = Color(0xFF6B7280), fontSize = 12.sp)
        }

        // ── Featured Bundle section ───────────────────────────────────────────
        if (store.bundles.isNotEmpty()) {
            item { SectionTitle(title = "FEATURED BUNDLE") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(store.bundles) { bundleInfo ->
//                        BundleCard(bundleInfo = bundleInfo, storeViewModel = storeViewModel)
                        BundleCard(
                            bundleInfo = bundleInfo,
                            storeViewModel = storeViewModel,
                            onClick = {
                                onBundleClick(
                                    bundleInfo.uuid,
                                    bundleInfo.durationRemainingInSeconds
                                )
                            }
                        )
                    }
                }
            }
        }

        // ── Daily Store section ───────────────────────────────────────────────
        item { SectionTitle(title = "DAILY STORE") }

        items(store.skinUuids) { skinUuid ->
            SkinOfferCard(
                skinUuid       = skinUuid,
                storeViewModel = storeViewModel,
                offerPrice     = store.skinPrices[skinUuid]
            )
        }
    }
}

// ── Section title ─────────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(text = title, color = Color(0xFF9BA3AF), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f).height(1.dp).background(
            Brush.horizontalGradient(listOf(Color(0xFFFF4655).copy(alpha = 0.4f), Color.Transparent))
        ))
    }
}

// ── Night Market chip ─────────────────────────────────────────────────────────

@Composable
private fun NightMarketChip(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF3A0550), Color(0xFF1A0030))))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Image(
                painter = painterResource(id = R.drawable.ic_nightmarket),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
//            Text("🌙", fontSize = 14.sp)
//            Text("Night Market", color = Color(0xFFD4A8FF), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Bundle card ───────────────────────────────────────────────────────────────

@Composable
private fun BundleCard(
    bundleInfo: BundleInfo,
    storeViewModel: StoreViewModel,
    onClick: () -> Unit = {}
) {
    var bundle by remember { mutableStateOf<BundleDetail?>(null) }
    LaunchedEffect(bundleInfo.uuid) { bundle = storeViewModel.getBundleDetail(bundleInfo.uuid) }

    val remainingSec = bundleInfo.durationRemainingInSeconds
    val days  = TimeUnit.SECONDS.toDays(remainingSec)
    val hours = TimeUnit.SECONDS.toHours(remainingSec) % 24
    val timerText = if (days > 0) "${days}d ${hours}h" else "${hours}h"

    Card(
        modifier  = Modifier.width(300.dp).height(200.dp).clickable { onClick() },
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A2332))) {
            val bgImage = bundle?.displayIcon2 ?: bundle?.displayIcon
            if (bgImage != null) {
                AsyncImage(model = bgImage, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xDD0F1923)))))

            Column(modifier = Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                Text(bundle?.displayName ?: "Loading...", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("⏱ $timerText left", color = Color(0xFF9BA3AF), fontSize = 11.sp)
                    if (bundle?.price != null && bundle!!.price!! > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            AsyncImage(model = VP_ICON_URL, contentDescription = "VP", modifier = Modifier.size(12.dp))
                            Text("${bundle!!.price}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            val weapons = bundle?.weapons?.take(3)
            if (!weapons.isNullOrEmpty()) {
                Column(modifier = Modifier.align(Alignment.TopEnd).padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.End) {
                    weapons.forEach { weapon ->
                        val imgUrl = weapon.fullRender ?: weapon.displayIcon
                        if (imgUrl != null) {
                            AsyncImage(model = imgUrl, contentDescription = weapon.displayName, modifier = Modifier.width(90.dp).height(28.dp), contentScale = ContentScale.Fit)
                        }
                    }
                }
            }
        }
    }
}

// ── Skin offer card ───────────────────────────────────────────────────────────

@Composable
private fun SkinOfferCard(
    skinUuid: String,
    storeViewModel: StoreViewModel,
    offerPrice: Int?
) {
    var skinName by remember { mutableStateOf("") }
    var iconUrl  by remember { mutableStateOf("https://media.valorant-api.com/weaponskinlevels/$skinUuid/displayicon.png") }
    var tierUuid  by remember { mutableStateOf<String?>(null) }
    var vpPrice   by remember { mutableStateOf<Int?>(offerPrice) }
    var videoUrl  by remember { mutableStateOf<String?>(null) }
    var showVideo by remember { mutableStateOf(false) }

    LaunchedEffect(skinUuid) {
        val info = storeViewModel.getSkinInfo(skinUuid)
        if (info != null) {
            skinName = info.displayName
            tierUuid = info.tierUuid
            videoUrl = info.videoUrl
            if (info.displayIcon != null) iconUrl = info.displayIcon
            if (vpPrice == null || vpPrice == 0) if (info.cost > 0) vpPrice = info.cost
        }
    }

    val tier = tierConfigFromUuid(tierUuid)

    Card(
        modifier  = Modifier.fillMaxWidth().height(200.dp)
            .then(if (videoUrl != null) Modifier.clickable { showVideo = true } else Modifier),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(tier.backgroundEnd, tier.backgroundStart)))) {
            AsyncImage(
                model = iconUrl, contentDescription = skinName,
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.78f).align(Alignment.Center).padding(horizontal = 20.dp, vertical = 8.dp),
                contentScale = ContentScale.Fit
            )
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (vpPrice != null && vpPrice!! > 0) {
                    AsyncImage(model = VP_ICON_URL, contentDescription = "VP", modifier = Modifier.size(15.dp))
                    Text(vpPrice.toString(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                if (tier.badgeRes != null) {
                    Icon(painter = painterResource(id = tier.badgeRes), contentDescription = null, modifier = Modifier.size(22.dp), tint = Color.Unspecified)
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = skinName.uppercase().ifEmpty { "..." },
                    color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    maxLines = 2, modifier = Modifier.align(Alignment.BottomStart).padding(end = if (videoUrl != null) 100.dp else 8.dp)
                )
                if (videoUrl != null) {
                    Text(
                        text = "▶  Tap to preview", color = Color(0xCCFFFFFF), fontSize = 10.sp,
                        modifier = Modifier.align(Alignment.BottomEnd)
                            .background(Color(0x55000000), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }

    if (showVideo && videoUrl != null) {
        SkinVideoDialog(skinName = skinName, videoUrl = videoUrl!!, onDismiss = { showVideo = false })
    }
}

// ── Balance chip ──────────────────────────────────────────────────────────────

@Composable
private fun BalanceChip(modifier: Modifier, label: String, iconUrl: String, amount: Int) {
    Row(
        modifier = modifier.background(Color(0xFF1A2332), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AsyncImage(model = iconUrl, contentDescription = label, modifier = Modifier.size(18.dp))
        Text(label, color = Color(0xFF9BA3AF), fontSize = 12.sp)
        Spacer(modifier = Modifier.weight(1f))
        Text(amount.toString(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

// ── States ────────────────────────────────────────────────────────────────────

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
        modifier = Modifier.fillMaxSize().padding(32.dp),
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