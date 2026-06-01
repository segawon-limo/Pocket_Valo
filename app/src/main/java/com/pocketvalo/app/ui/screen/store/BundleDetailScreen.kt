package com.pocketvalo.app.ui.screen.store

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.pocketvalo.app.data.model.BundleDetail
import com.pocketvalo.app.data.repository.AssetsRepository
import com.pocketvalo.app.data.repository.BundleSkinItem
import com.pocketvalo.app.ui.viewmodel.StoreViewModel
import java.util.concurrent.TimeUnit

@Composable
fun BundleDetailScreen(
    bundleUuid: String,
    durationRemainingSeconds: Long,
    totalDiscountedPrice: Int = 0,
    totalBasePrice: Int = 0,
    skinItems: List<BundleSkinItem> = emptyList(),
    storeViewModel: StoreViewModel,
    navController: NavController
) {
    var bundleMeta by remember { mutableStateOf<BundleDetail?>(null) }
    var skinInfoList by remember { mutableStateOf<List<Pair<BundleSkinItem, AssetsRepository.StoreSkinInfo?>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val days    = TimeUnit.SECONDS.toDays(durationRemainingSeconds)
    val hours   = TimeUnit.SECONDS.toHours(durationRemainingSeconds) % 24
    val timerText = if (days > 0) "${days}d ${hours}h left" else "${hours}h left"

    LaunchedEffect(bundleUuid) {
        // Fetch metadata bundle (nama, gambar background)
        bundleMeta = storeViewModel.getBundleDetail(bundleUuid)

        // Resolve skin item UUIDs ke skin info (nama, icon, tier)
        skinInfoList = skinItems.map { item ->
            item to storeViewModel.getSkinInfo(item.itemId)
        }
        isLoading = false

        android.util.Log.d("BundleDetail", "bundleMeta: ${bundleMeta?.displayName}")
        android.util.Log.d("BundleDetail", "skinItems: ${skinItems.size}")
        android.util.Log.d("BundleDetail", "skinInfoList: ${skinInfoList.map { (_, info) -> info?.displayName ?: "null" }}")
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0F1923))
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFFFF4655))
            return@Box
        }

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            // ── Header ────────────────────────────────────────────────────────
            item {
                Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                    val bgUrl = bundleMeta?.displayIcon2 ?: bundleMeta?.displayIcon
                    if (bgUrl != null) {
                        AsyncImage(model = bgUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A2332)))
                    }

                    Box(modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color(0x33000000), Color(0xFF0F1923)))
                    ))

                    IconButton(
                        onClick  = { navController.popBackStack() },
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                        Text(bundleMeta?.displayName ?: "", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("⏱ $timerText", color = Color(0xFF9BA3AF), fontSize = 13.sp)
                            if (totalDiscountedPrice > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    AsyncImage(model = VP_ICON_URL, contentDescription = "VP", modifier = Modifier.size(14.dp))
                                    if (totalBasePrice > totalDiscountedPrice) {
                                        Text("$totalBasePrice", color = Color(0xFF6B7280), fontSize = 11.sp,
                                            style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough))
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text("$totalDiscountedPrice", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // ── Description ───────────────────────────────────────────────────
            if (!bundleMeta?.description.isNullOrBlank() && bundleMeta?.description != bundleMeta?.displayName) {
                item {
                    Text(bundleMeta!!.description!!, color = Color(0xFF9BA3AF), fontSize = 13.sp, lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
            }

            // ── Weapons section ───────────────────────────────────────────────
            if (skinInfoList.isNotEmpty()) {
                item { SectionDivider("INCLUDED SKINS") }
                items(skinInfoList) { (bundleItem, skinInfo) ->
                    BundleSkinCard(bundleItem = bundleItem, skinInfo = skinInfo)
                }
            } else if (!isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No skin info available", color = Color(0xFF4A5568), fontSize = 13.sp)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

// ── Section divider ───────────────────────────────────────────────────────────

@Composable
private fun SectionDivider(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(title, color = Color(0xFF9BA3AF), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f).height(1.dp).background(
            Brush.horizontalGradient(listOf(Color(0xFFFF4655).copy(alpha = 0.4f), Color.Transparent))
        ))
    }
}

// ── Bundle skin card ──────────────────────────────────────────────────────────

@Composable
private fun BundleSkinCard(
    bundleItem: BundleSkinItem,
    skinInfo: AssetsRepository.StoreSkinInfo?
) {
    var showVideo by remember { mutableStateOf(false) }
    val videoUrl = skinInfo?.videoUrl
    val tier     = tierConfigFromUuid(skinInfo?.tierUuid)

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .then(if (videoUrl != null) Modifier.clickable { showVideo = true } else Modifier),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(tier.backgroundEnd, tier.backgroundStart))
        )) {
            // Skin image
            val iconUrl = skinInfo?.displayIcon
            if (iconUrl != null) {
                AsyncImage(
                    model              = iconUrl,
                    contentDescription = skinInfo.displayName,
                    modifier           = Modifier.fillMaxWidth().fillMaxHeight(0.78f).align(Alignment.Center).padding(horizontal = 20.dp, vertical = 8.dp),
                    contentScale       = ContentScale.Fit
                )
            }

            // Price + tier badge top-right
            Row(
                modifier              = Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (bundleItem.discountedPrice > 0) {
                    AsyncImage(model = VP_ICON_URL, contentDescription = "VP", modifier = Modifier.size(14.dp))
                    if (bundleItem.basePrice > bundleItem.discountedPrice) {
                        Text("${bundleItem.basePrice}", color = Color(0xFF9BA3AF), fontSize = 11.sp,
                            style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough))
                    }
                    Text("${bundleItem.discountedPrice}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                } else {
                    // Melee di bundle biasanya gratis
                    Text(
                        text       = "Free in Bundle",
                        color      = Color(0xFF9BA3AF),
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (tier.badgeRes != null) {
                    androidx.compose.ui.res.painterResource(id = tier.badgeRes).let { painter ->
                        androidx.compose.material3.Icon(painter = painter, contentDescription = null, modifier = Modifier.size(22.dp), tint = Color.Unspecified)
                    }
                }
            }

            // Bottom: skin name + preview hint
            Box(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text       = (skinInfo?.displayName ?: bundleItem.itemId.take(8)).uppercase(),
                    color      = Color.White,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier.align(Alignment.BottomStart).padding(end = if (videoUrl != null) 110.dp else 8.dp)
                )
                if (videoUrl != null) {
                    Text(
                        text     = "▶  Tap to preview",
                        color    = Color(0xCCFFFFFF),
                        fontSize = 10.sp,
                        modifier = Modifier.align(Alignment.BottomEnd)
                            .background(Color(0x55000000), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }

    if (showVideo && videoUrl != null) {
        SkinVideoDialog(skinName = skinInfo?.displayName ?: "", videoUrl = videoUrl, onDismiss = { showVideo = false })
    }
}