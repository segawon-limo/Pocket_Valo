package com.pocketvalo.app.ui.screen.account

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.pocketvalo.app.R
import com.pocketvalo.app.data.local.MultiAccountTokenStorage
import com.pocketvalo.app.data.local.entity.AccountEntity
import com.pocketvalo.app.ui.viewmodel.AccountUiState
import com.pocketvalo.app.ui.viewmodel.AccountViewModel
import com.pocketvalo.app.ui.viewmodel.PlayerStats
import com.pocketvalo.app.ui.viewmodel.PlayerViewModel

private val CARD_WIDTH          = 245.dp
private val CARD_HEIGHT         = (245 * 640f / 268f).dp
private val PNG_RENDERED_WIDTH  = 275.dp
private val PNG_RENDERED_HEIGHT = (275 * 1080f / 478f).dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    playerViewModel: PlayerViewModel,
    accountViewModel: AccountViewModel = viewModel(),
    onAddAccount: () -> Unit = {}
) {
    val context       = androidx.compose.ui.platform.LocalContext.current
    val multiStorage  = remember { MultiAccountTokenStorage(context) }

    val uiState       by accountViewModel.uiState.collectAsState()
    val playerUiState by playerViewModel.uiState.collectAsState()

    // Refresh setiap kali AccountScreen di-enter — pastikan username & activePuuid sinkron
    // setelah switch account (AccountViewModel tidak re-create, jadi perlu manual refresh)
    LaunchedEffect(Unit) {
        accountViewModel.refresh()
    }

    LaunchedEffect(playerUiState.accountData, playerUiState.rawMatchHistory, playerUiState.matchHistory) {
        val account  = playerUiState.accountData
        val username = account?.let { "${it.name}#${it.tag}" }
        val lastMatch   = playerUiState.rawMatchHistory.firstOrNull()
        val playerMatch = lastMatch?.players?.allPlayers
            ?.firstOrNull { "${it.name}#${it.tag}" == username }
        val rankName = playerMatch?.rankName
        val tierData = playerUiState.rankTiers.values
            .firstOrNull { it.tierName.equals(rankName, ignoreCase = true) }
        accountViewModel.setPlayerData(
            playerCardUrl   = account?.card?.large,
            accountLevel    = account?.accountLevel,
            rankName        = playerUiState.currentRankName ?: rankName,
            rankIconUrl     = playerUiState.currentRankIconUrl ?: tierData?.smallIcon,
            currentRR       = playerUiState.currentRR,
            matchHistory    = playerUiState.matchHistory,
            rawMatchHistory = playerUiState.rawMatchHistory,
            maps            = playerUiState.maps
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
    ) {
        PlayerCardContent(
            uiState      = uiState,
            onSwitchClick = { accountViewModel.showAccountSheet() }
        )
    }

    // Account switcher bottom sheet
    // Baca activePuuid langsung dari multiStorage — lebih reliable dari uiState.puuid yang bisa stale
    val activePuuid = multiStorage.activePuuid ?: uiState.puuid

    if (uiState.showAccountSheet) {
        ModalBottomSheet(
            onDismissRequest  = { accountViewModel.dismissAccountSheet() },
            containerColor    = Color(0xFF1A2332),
            scrimColor        = Color.Black.copy(alpha = 0.6f),
            shape             = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            AccountSwitcherSheet(
                uiState          = uiState,
                activePuuid      = activePuuid,
                onSwitch         = { accountViewModel.switchToAccount(it) },
                onRemove         = { accountViewModel.removeAccount(it) },
                onAddAccount     = {
                    accountViewModel.dismissAccountSheet()
                    onAddAccount()
                }
            )
        }
    }
}

// ── Account switcher sheet ────────────────────────────────────────────────────

@Composable
private fun AccountSwitcherSheet(
    uiState: AccountUiState,
    activePuuid: String?,
    onSwitch: (AccountEntity) -> Unit,
    onRemove: (AccountEntity) -> Unit,
    onAddAccount: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        // Handle
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
            text       = "ACCOUNTS",
            color      = Color(0xFF9BA3AF),
            fontSize   = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.switchError != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF3A0515), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(text = uiState.switchError, color = Color(0xFFFF4655), fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (uiState.isSwitching) {
            Box(
                modifier         = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        color       = Color(0xFFFF4655),
                        strokeWidth = 2.dp
                    )
                    Text("Switching account...", color = Color(0xFF9BA3AF), fontSize = 13.sp)
                }
            }
        } else {
            // Account list
            uiState.savedAccounts.forEach { account ->
                val isActive = account.puuid == activePuuid
                AccountRow(
                    account  = account,
                    isActive = isActive,
                    onSwitch = { if (!isActive) onSwitch(account) },
                    onRemove = { if (!isActive) onRemove(account) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Add account button
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
                Icon(
                    imageVector        = Icons.Default.Add,
                    contentDescription = "Add account",
                    tint               = Color(0xFFFF4655),
                    modifier           = Modifier.size(20.dp)
                )
            }
            Text(
                text       = "Add Account",
                color      = Color(0xFFFF4655),
                fontSize   = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AccountRow(
    account: AccountEntity,
    isActive: Boolean,
    onSwitch: () -> Unit,
    onRemove: () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isActive) Color(0xFF1E3A2E) else Color(0xFF0F1923)
            )
            .clickable(enabled = !isActive) { onSwitch() }
            .padding(12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar / card image
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
                    modifier           = Modifier.fillMaxSize(),
                    contentScale       = ContentScale.Crop
                )
            }
        }

        // Name + region
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = account.riotId,
                color      = if (isActive) Color(0xFF4ADE80) else Color.White,
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

        // Active checkmark or remove button
        if (isActive) {
            Icon(
                imageVector        = Icons.Default.Check,
                contentDescription = "Active",
                tint               = Color(0xFF4ADE80),
                modifier           = Modifier.size(18.dp)
            )
        } else {
            if (showConfirm) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { showConfirm = false },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Cancel", color = Color(0xFF9BA3AF), fontSize = 12.sp)
                    }
                    TextButton(
                        onClick = { onRemove(); showConfirm = false },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Remove", color = Color(0xFFFF4655), fontSize = 12.sp)
                    }
                }
            } else {
                IconButton(
                    onClick  = { showConfirm = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.Delete,
                        contentDescription = "Remove account",
                        tint               = Color(0xFF4A5568),
                        modifier           = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ── Player card + stats ───────────────────────────────────────────────────────

@Composable
private fun PlayerCardContent(
    uiState: AccountUiState,
    onSwitchClick: () -> Unit
) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // ── Outer Box: level badge + card + rank ──────────────────────────────
        Box(
            modifier         = Modifier
                .width(PNG_RENDERED_WIDTH)
                .height(PNG_RENDERED_HEIGHT + 60.dp + 60.dp),
            contentAlignment = Alignment.Center
        ) {
            // Card Box
            Box(
                modifier         = Modifier
                    .width(PNG_RENDERED_WIDTH)
                    .height(PNG_RENDERED_HEIGHT)
                    .align(Alignment.Center),
                contentAlignment = Alignment.TopCenter
            ) {
                // Layer 1 — Card art
                if (uiState.playerCardUrl != null) {
                    AsyncImage(
                        model              = uiState.playerCardUrl,
                        contentDescription = "Player Card",
                        modifier           = Modifier
                            .width(CARD_WIDTH)
                            .height(CARD_HEIGHT)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale       = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .width(CARD_WIDTH)
                            .height(CARD_HEIGHT)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1A2332))
                    )
                }
                // Layer 2 — Fade
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.80f)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xFF0F1923))
                            )
                        )
                )
                // Layer 3 — PNG ornamen
                Image(
                    painter            = painterResource(id = R.drawable.player_card_frame),
                    contentDescription = null,
                    modifier           = Modifier
                        .width(PNG_RENDERED_WIDTH)
                        .height(PNG_RENDERED_HEIGHT)
                        .align(Alignment.BottomCenter),
                    contentScale       = ContentScale.FillWidth
                )
                // Layer 5 — Nama + title
                Column(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 184.dp, start = 12.dp, end = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text       = uiState.username ?: "",
                        color      = Color(0xFF1A1A1A),
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign  = TextAlign.Center
                    )
                    if (uiState.isTitleLoading) {
                        Spacer(modifier = Modifier.height(1.dp))
                        CircularProgressIndicator(
                            modifier    = Modifier.size(10.dp),
                            color       = Color(0xFF4A4A4A),
                            strokeWidth = 1.dp
                        )
                    } else if (uiState.titleText != null) {
                        Text(
                            text      = uiState.titleText,
                            color     = Color(0xFFEFEFAE),
                            fontSize  = 10.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Level badge — TopCenter
            if (uiState.accountLevel != null) {
                Box(
                    modifier         = Modifier
                        .align(Alignment.TopCenter)
                        .size(60.dp)
                        .offset(y = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.levelNumberAppearanceUrl != null) {
                        AsyncImage(
                            model              = uiState.levelNumberAppearanceUrl,
                            contentDescription = "Level Border",
                            modifier           = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color(0xFF1A2332), RoundedCornerShape(3.dp))
                        )
                    }
                    Text(
                        text       = "${uiState.accountLevel}",
                        color      = Color.White,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Rank — BottomCenter
            if (uiState.rankName != null || uiState.rankIconUrl != null) {
                Row(
                    modifier              = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 90.dp, start = 12.dp, end = 12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.rankIconUrl != null) {
                        AsyncImage(
                            model              = uiState.rankIconUrl,
                            contentDescription = "Rank",
                            modifier           = Modifier.size(80.dp)
                        )
                    } else if (uiState.rankName != null) {
                        Text(
                            text       = if (uiState.rankName == "Unrated") "?" else uiState.rankName,
                            color      = Color.White,
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Switch account button
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick  = onSwitchClick,
            shape    = RoundedCornerShape(8.dp),
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF9BA3AF)),
            border   = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A3A4A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text       = "Switch Account",
                fontSize   = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Stats section ─────────────────────────────────────────────────────
        if (uiState.stats != null && uiState.stats.totalMatches > 0) {
            StatsSection(stats = uiState.stats)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ── Stats Section ─────────────────────────────────────────────────────────────

@Composable
private fun StatsSection(stats: PlayerStats) {
    Column(
        modifier            = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.fillMaxWidth()
        ) {
            Text(
                text          = "RECENT PERFORMANCE",
                color         = Color(0xFF9BA3AF),
                fontSize      = 11.sp,
                fontWeight    = FontWeight.SemiBold,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFFF4655).copy(alpha = 0.5f), Color.Transparent)
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        WinRateCard(stats = stats)

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KdaCard(modifier = Modifier.weight(1f), label = "KILLS",   value = String.format("%.1f", stats.avgKills),   color = Color(0xFFFF4655), accent = Color(0xFFFF4655).copy(alpha = 0.15f))
            KdaCard(modifier = Modifier.weight(1f), label = "DEATHS",  value = String.format("%.1f", stats.avgDeaths),  color = Color(0xFF9BA3AF), accent = Color(0xFF9BA3AF).copy(alpha = 0.1f),  sub = "K/D ${String.format("%.2f", stats.kdRatio)}")
            KdaCard(modifier = Modifier.weight(1f), label = "ASSISTS", value = String.format("%.1f", stats.avgAssists), color = Color(0xFFFBBF24), accent = Color(0xFFFBBF24).copy(alpha = 0.12f))
        }

        ShotDistributionCard(stats = stats)

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AgentCard(stats = stats, modifier = Modifier.weight(1f))
            MapCard(stats = stats, modifier = Modifier.weight(1f))
        }
    }
}

// ── Win Rate card ─────────────────────────────────────────────────────────────

@Composable
private fun WinRateCard(stats: PlayerStats) {
    val animatedWinRate by animateFloatAsState(targetValue = stats.winRate, animationSpec = tween(1000), label = "win_rate_anim")
    val winColor = if (stats.winRate >= 0.5f) Color(0xFF4ADE80) else Color(0xFFFF4655)

    Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF1A2332), RoundedCornerShape(12.dp))) {
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Brush.horizontalGradient(listOf(Color(0xFFFF4655), Color.Transparent))))
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(color = Color(0xFF1E2D3D), startAngle = -90f, sweepAngle = 360f, useCenter = false, style = stroke)
                    drawArc(color = winColor, startAngle = -90f, sweepAngle = animatedWinRate * 360f, useCenter = false, style = stroke)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${(stats.winRate * 100).toInt()}%", color = winColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
                    Text(text = "WIN", color = Color(0xFF9BA3AF), fontSize = 9.sp, letterSpacing = 1.sp)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Last ${stats.totalMatches} Games", color = Color(0xFF9BA3AF), fontSize = 10.sp, letterSpacing = 0.5.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WLBox(modifier = Modifier.weight(1f), count = stats.wins, label = "WINS", color = Color(0xFF4ADE80))
                    WLBox(modifier = Modifier.weight(1f), count = stats.totalMatches - stats.wins, label = "LOSSES", color = Color(0xFFFF4655))
                }
                Text(text = "Competitive · Unrated", color = Color(0xFF6B7280), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun WLBox(modifier: Modifier, count: Int, label: String, color: Color) {
    Column(modifier = modifier.background(Color(0xFF0F1923), RoundedCornerShape(6.dp)).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = "$count", color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = Color(0xFF6B7280), fontSize = 9.sp, letterSpacing = 0.5.sp)
    }
}

// ── KDA card ──────────────────────────────────────────────────────────────────

@Composable
private fun KdaCard(modifier: Modifier, label: String, value: String, color: Color, accent: Color, sub: String? = null) {
    Column(modifier = modifier.background(Color(0xFF1A2332), RoundedCornerShape(10.dp)).clip(RoundedCornerShape(10.dp)), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(modifier = Modifier.padding(12.dp, 12.dp, 12.dp, 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = label, color = Color(0xFF6B7280), fontSize = 9.sp, letterSpacing = 1.sp)
            Text(text = value, color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
            if (sub != null) Text(text = sub, color = Color(0xFF4ADE80), fontSize = 10.sp, fontWeight = FontWeight.Medium)
            else Spacer(modifier = Modifier.height(10.sp.value.dp))
        }
        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(color))
    }
}

// ── Shot distribution ─────────────────────────────────────────────────────────

@Composable
private fun ShotDistributionCard(stats: PlayerStats) {
    val hsColor = Color(0xFFFBBF24); val bodyColor = Color(0xFF3B82F6); val legColor = Color(0xFF6B7280)
    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF1A2332), RoundedCornerShape(10.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = "SHOT DISTRIBUTION", color = Color(0xFF9BA3AF), fontSize = 10.sp, letterSpacing = 1.5.sp)
            Text(text = "${(stats.headshotPct * 100).toInt()}% HS", color = hsColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Row(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (stats.headshotPct > 0f) Box(modifier = Modifier.weight(stats.headshotPct.coerceAtLeast(0.01f)).fillMaxHeight().background(hsColor, RoundedCornerShape(3.dp)))
            if (stats.bodyshotPct > 0f) Box(modifier = Modifier.weight(stats.bodyshotPct.coerceAtLeast(0.01f)).fillMaxHeight().background(bodyColor, RoundedCornerShape(3.dp)))
            val leg = stats.legshotPct.coerceAtLeast(0f)
            if (leg > 0f) Box(modifier = Modifier.weight(leg.coerceAtLeast(0.01f)).fillMaxHeight().background(legColor, RoundedCornerShape(3.dp)))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            listOf(Triple(hsColor, "Head", "${(stats.headshotPct * 100).toInt()}%"), Triple(bodyColor, "Body", "${(stats.bodyshotPct * 100).toInt()}%"), Triple(legColor, "Leg", "${(stats.legshotPct * 100).toInt()}%")).forEach { (color, label, pct) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(6.dp).background(color, RoundedCornerShape(50)))
                    Text(text = "$label $pct", color = Color(0xFF6B7280), fontSize = 9.sp)
                }
            }
        }
    }
}

// ── Agent card ────────────────────────────────────────────────────────────────

@Composable
private fun AgentCard(stats: PlayerStats, modifier: Modifier) {
    Box(modifier = modifier.height(90.dp).background(Color(0xFF1A2332), RoundedCornerShape(10.dp)).clip(RoundedCornerShape(10.dp))) {
        if (stats.mostPlayedAgentUrl != null) {
            AsyncImage(model = stats.mostPlayedAgentUrl, contentDescription = null, modifier = Modifier.fillMaxHeight().width(60.dp).align(Alignment.CenterEnd), contentScale = ContentScale.Crop)
            Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF1A2332), Color(0xFF1A2332).copy(alpha = 0.3f)))))
        }
        Column(modifier = Modifier.align(Alignment.CenterStart).padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = "TOP AGENT", color = Color(0xFF9BA3AF), fontSize = 9.sp, letterSpacing = 1.5.sp)
            Text(text = stats.mostPlayedAgent ?: "-", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp)
            Text(text = "${stats.mostPlayedAgentGames} games", color = Color(0xFF6B7280), fontSize = 10.sp)
        }
    }
}

// ── Map card ──────────────────────────────────────────────────────────────────

@Composable
private fun MapCard(stats: PlayerStats, modifier: Modifier) {
    Box(
        modifier = modifier
            .height(90.dp)
            .background(Color(0xFF1A2332), RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
    ) {
        if (stats.mostPlayedMapImageUrl != null) {
            AsyncImage(
                model              = stats.mostPlayedMapImageUrl,
                contentDescription = null,
                modifier           = Modifier.fillMaxSize(),
                contentScale       = ContentScale.Crop
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xFF1A2332),
                            0.5f to Color(0xFF1A2332).copy(alpha = 0.85f),
                            1.0f to Color(0xFF1A2332).copy(alpha = 0.3f)
                        )
                    )
                )
        )
        Column(
            modifier            = Modifier.align(Alignment.CenterStart).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = "TOP MAP", color = Color(0xFF9BA3AF), fontSize = 9.sp, letterSpacing = 1.5.sp)
            Text(text = stats.mostPlayedMap ?: "-", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp)
            Text(
                text       = "${stats.mostPlayedMapWins}W · ${stats.mostPlayedMapLosses}L",
                color      = if (stats.mostPlayedMapWins >= stats.mostPlayedMapLosses) Color(0xFF4ADE80) else Color(0xFFFF4655),
                fontSize   = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}