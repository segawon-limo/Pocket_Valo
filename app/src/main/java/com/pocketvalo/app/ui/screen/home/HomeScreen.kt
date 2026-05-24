package com.pocketvalo.app.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.pocketvalo.app.data.local.entity.MatchEntity
import com.pocketvalo.app.data.model.TierData
import com.pocketvalo.app.ui.navigation.Screen
import com.pocketvalo.app.ui.viewmodel.PlayerViewModel

@Composable
fun HomeScreen(
    navController: NavController,
    playerViewModel: PlayerViewModel
) {
    val uiState by playerViewModel.uiState.collectAsStateWithLifecycle()

    // Jika user masuk via Riot auth (bukan InputScreen), trigger load dari TokenStorage
    LaunchedEffect(Unit) {
        playerViewModel.loadFromTokenIfNeeded()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
    ) {
        when {
            uiState.isLoading -> {
                HomeScreenSkeleton()
            }
            uiState.error != null -> {
                Text(
                    text = uiState.error!!,
                    color = Color(0xFFFF4655),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    item {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier              = Modifier.padding(bottom = 4.dp)
                        ) {
                            // Player card display icon
                            if (uiState.playerCardSmallUrl != null) {
                                AsyncImage(
                                    model              = uiState.playerCardSmallUrl,
                                    contentDescription = "Player Card",
                                    modifier           = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    contentScale       = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF1A2332))
                                )
                            }

                            // Nama + level
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text     = "Welcome back",
                                    color    = Color(0xFF9BA3AF),
                                    fontSize = 14.sp
                                )
                                Text(
                                    text       = if (uiState.accountData != null)
                                        "${uiState.accountData!!.name}#${uiState.accountData!!.tag}"
                                    else "PlayerName#TAG",
                                    color      = Color.White,
                                    fontSize   = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                uiState.accountData?.let {
                                    Text(
                                        text     = "Level ${it.accountLevel} · ${it.region.uppercase()}",
                                        color    = Color(0xFF9BA3AF),
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            // Current rank — kanan header
                            if (uiState.currentRankIconUrl != null || uiState.currentRankName != null) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    if (uiState.currentRankIconUrl != null) {
                                        AsyncImage(
                                            model              = uiState.currentRankIconUrl,
                                            contentDescription = "Rank",
                                            modifier           = Modifier.size(50.dp)
                                        )
                                    }
                                    val rankName = uiState.currentRankName
                                    if (rankName != null) {
                                        Text(
                                            text      = rankName,
                                            color     = Color(0xFF9BA3AF),
                                            fontSize  = 17.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
//                                    if (uiState.currentRR != null) {
//                                        Text(
//                                            text      = "${uiState.currentRR} RR",
//                                            color     = Color.White,
//                                            fontSize  = 11.sp,
//                                            fontWeight = FontWeight.SemiBold,
//                                            textAlign = TextAlign.Center
//                                        )
//                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text       = "Recent Matches",
                            color      = Color.White,
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (uiState.matchHistory.isEmpty()) {
                        item {
                            Text(
                                text = "No matches found",
                                color = Color(0xFF9BA3AF),
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        items(uiState.matchHistory) { match ->
                            MatchCard(
                                match = match,
                                rankTiers = uiState.rankTiers,
                                onClick = {
                                    navController.navigate(Screen.Match.createRoute(match.matchId))
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatchCard(
    match: MatchEntity,
    rankTiers: Map<String, TierData>,
    onClick: () -> Unit
) {
    val roundsWon = if (match.playerTeam.equals("Red", ignoreCase = true))
        match.redRoundsWon else match.blueRoundsWon
    val roundsLost = if (match.playerTeam.equals("Red", ignoreCase = true))
        match.blueRoundsWon else match.redRoundsWon

    val isDraw    = roundsWon == roundsLost
    val isVictory = match.hasWon && !isDraw

    val cardColor   = when {
        isDraw    -> Color(0xFF2A2A2A)
        isVictory -> Color(0xFF1A3A2A)
        else      -> Color(0xFF3A1A1A)
    }
    val resultColor = when {
        isDraw    -> Color(0xFF9BA3AF)
        isVictory -> Color(0xFF4ADE80)
        else      -> Color(0xFFFF4655)
    }
    val resultText = when {
        isDraw    -> "DRAW"
        isVictory -> "VICTORY"
        else      -> "DEFEAT"
    }

    val rankKey = match.rankName?.uppercase()
    val rankTier = rankTiers[rankKey]

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Agent portrait
            if (match.agentPortraitUrl != null) {
                AsyncImage(
                    model = match.agentPortraitUrl,
                    contentDescription = match.agentName,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF374151)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = match.agentName.firstOrNull()?.toString() ?: "?",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Rank icon
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                if (rankTier?.smallIcon != null) {
                    AsyncImage(
                        model = rankTier.smallIcon,
                        contentDescription = rankTier.tierName,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // KDA + info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "KDA ${match.kills} / ${match.deaths} / ${match.assists}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "SCORE ${match.score}",
                    color = Color(0xFF9BA3AF),
                    fontSize = 12.sp
                )
                Text(
                    text = "${match.mode} · ${match.map}",
                    color = Color(0xFF9BA3AF),
                    fontSize = 11.sp
                )
            }

            // Result
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = resultText,
                    color = resultColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$roundsWon - $roundsLost",
                    color = resultColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}