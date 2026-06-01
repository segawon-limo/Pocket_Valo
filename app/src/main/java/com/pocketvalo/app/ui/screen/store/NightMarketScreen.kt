package com.pocketvalo.app.ui.screen.store

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pocketvalo.app.data.repository.NightMarketOffer
import com.pocketvalo.app.ui.viewmodel.StoreViewModel
import java.util.concurrent.TimeUnit

// TierConfig, tierConfigFromUuid, VP_ICON_URL — defined in TierConfig.kt (same package)

@Composable
fun NightMarketScreen(
    storeViewModel: StoreViewModel,
    onBack: () -> Unit
) {
    val uiState by storeViewModel.uiState.collectAsState()
    val store   = uiState.store
    val offers  = store?.nightMarketOffers ?: emptyList()

    val remainingSec = store?.nightMarketRemainingSeconds ?: 0L
    val days    = TimeUnit.SECONDS.toDays(remainingSec)
    val hours   = TimeUnit.SECONDS.toHours(remainingSec) % 24
    val minutes = TimeUnit.SECONDS.toMinutes(remainingSec) % 60
    val timerText = when {
        days > 0  -> "${days}d ${hours}h ${minutes}m"
        hours > 0 -> "${hours}h ${minutes}m"
        else      -> "${minutes}m"
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F1923))) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {

            // ── Header ────────────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Brush.verticalGradient(listOf(Color(0xFF3A0550), Color(0xFF0F1923))))
                ) {
                    Column(
                        modifier = Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 20.dp)
                    ) {
                        Text(
                            text       = "🌙  NIGHT MARKET",
                            color      = Color(0xFFD4A8FF),
                            fontSize   = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        if (remainingSec > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Ends in $timerText", color = Color(0xFF9B6FBF), fontSize = 13.sp)
                        }
                    }
                    IconButton(
                        onClick  = onBack,
                        modifier = Modifier.align(Alignment.TopStart).padding(top = 8.dp, start = 4.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
            }

            // ── Offers ────────────────────────────────────────────────────────
            if (offers.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Text("No Night Market offers available.", color = Color(0xFF6B7280), fontSize = 14.sp)
                    }
                }
            } else {
                items(offers) { offer ->
                    NightMarketOfferCard(
                        offer          = offer,
                        storeViewModel = storeViewModel,
                        modifier       = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NightMarketOfferCard(
    offer: NightMarketOffer,
    storeViewModel: StoreViewModel,
    modifier: Modifier = Modifier
) {
    var skinName by remember { mutableStateOf("") }
    var iconUrl  by remember {
        mutableStateOf("https://media.valorant-api.com/weaponskinlevels/${offer.skinUuid}/displayicon.png")
    }
    var tierUuid by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(offer.skinUuid) {
        val info = storeViewModel.getSkinInfo(offer.skinUuid)
        if (info != null) {
            skinName = info.displayName
            tierUuid = info.tierUuid
            if (info.displayIcon != null) iconUrl = info.displayIcon
        }
    }

    val tier = tierConfigFromUuid(tierUuid)

    Card(
        modifier  = modifier.fillMaxWidth().height(180.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(tier.backgroundEnd, tier.backgroundStart))
        )) {
            // Skin image
            AsyncImage(
                model = iconUrl, contentDescription = skinName,
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.75f).align(Alignment.Center).padding(horizontal = 20.dp, vertical = 8.dp),
                contentScale = ContentScale.Fit
            )

            // Discount badge
            Box(
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF8B00FF), Color(0xFFFF00AA))))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(text = "-${offer.discountPercent}%", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }

            // Tier badge
            if (tier.badgeRes != null) {
                Icon(
                    painter = painterResource(id = tier.badgeRes),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(22.dp),
                    tint = Color.Unspecified
                )
            }

            // Bottom: nama + harga
            Box(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = skinName.uppercase().ifEmpty { "..." },
                    color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.align(Alignment.BottomStart).padding(end = 120.dp)
                )
                Row(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (offer.originalPrice > 0) {
                        Text(
                            text = "${offer.originalPrice}",
                            color = Color(0xFF9BA3AF),
                            fontSize = 11.sp,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                    AsyncImage(model = VP_ICON_URL, contentDescription = "VP", modifier = Modifier.size(14.dp))
                    Text(text = "${offer.discountedPrice}", color = Color(0xFFD4A8FF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}