package com.pocketvalo.app.ui.screen.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.pocketvalo.app.R
import com.pocketvalo.app.ui.viewmodel.AccountUiState
import com.pocketvalo.app.ui.viewmodel.AccountViewModel
import com.pocketvalo.app.ui.viewmodel.PlayerViewModel

private val CARD_WIDTH          = 245.dp
private val CARD_HEIGHT         = (245 * 640f / 268f).dp
private val PNG_RENDERED_WIDTH  = 275.dp
private val PNG_RENDERED_HEIGHT = (275 * 1080f / 478f).dp

@Composable
fun AccountScreen(
    playerViewModel: PlayerViewModel,
    accountViewModel: AccountViewModel = viewModel()
) {
    val uiState       by accountViewModel.uiState.collectAsState()
    val playerUiState by playerViewModel.uiState.collectAsState()

    LaunchedEffect(playerUiState.accountData, playerUiState.rawMatchHistory) {
        val account  = playerUiState.accountData
        val username = account?.let { "${it.name}#${it.tag}" }
        val lastMatch   = playerUiState.rawMatchHistory.firstOrNull()
        val playerMatch = lastMatch?.players?.allPlayers
            ?.firstOrNull { "${it.name}#${it.tag}" == username }
        val rankName = playerMatch?.rankName
        val tierData = playerUiState.rankTiers.values
            .firstOrNull { it.tierName.equals(rankName, ignoreCase = true) }
        accountViewModel.setPlayerData(
            playerCardUrl = account?.card?.large,
            accountLevel  = account?.accountLevel,
            rankName      = rankName,
            rankIconUrl   = tierData?.smallIcon
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
    ) {
        PlayerCardContent(uiState = uiState)
    }
}

@Composable
private fun PlayerCardContent(uiState: AccountUiState) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ── Outer Box: level badge + card + rank ──────────────────────────────
        Box(
            modifier         = Modifier
                .width(PNG_RENDERED_WIDTH)
                .height(PNG_RENDERED_HEIGHT + 30.dp + 40.dp), // card + level + rank
            contentAlignment = Alignment.Center
        ) {
            // ── Card Box ──────────────────────────────────────────────────────
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

                // Layer 2 — Fade bawah
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

                // Layer 4 — Nama + tag + title
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

            // ── Level badge — overlap di atas card ────────────────────────────
            if (uiState.accountLevel != null) {
                Box(
                    modifier         = Modifier
                        .align(Alignment.TopCenter)
                        .size(30.dp)
//                        .padding(top = 30.dp, start = 12.dp, end = 12.dp)
                        .offset(y = (30.dp))
                        .background(Color(0xFF1A2332), RoundedCornerShape(3.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = "${uiState.accountLevel}",
                        color      = Color.White,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ── Rank — overlap di bawah card ──────────────────────────────────
            if (uiState.rankName != null || uiState.rankIconUrl != null) {
                Row(
                    modifier              = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 70.dp, start = 12.dp, end = 12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.rankIconUrl != null) {
                        AsyncImage(
                            model = uiState.rankIconUrl,
                            contentDescription = "Rank",
                            modifier = Modifier.size(80.dp)
                        )
                    } else if (uiState.rankName != null) {
                        Text(
                            text = if (uiState.rankName == "Unrated") {
                                "?"
                            } else {
                                uiState.rankName
                            },
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}