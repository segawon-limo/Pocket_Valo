package com.pocketvalo.app.ui.screen.match

import com.pocketvalo.app.util.formatMatchDateTime
import androidx.compose.foundation.background
import com.pocketvalo.app.util.formatMatchDateTime
import androidx.compose.foundation.border
import com.pocketvalo.app.util.formatMatchDateTime
import androidx.compose.foundation.horizontalScroll
import com.pocketvalo.app.util.formatMatchDateTime
import androidx.compose.foundation.layout.*
import com.pocketvalo.app.util.formatMatchDateTime
import androidx.compose.foundation.lazy.LazyColumn
import com.pocketvalo.app.util.formatMatchDateTime
import androidx.compose.foundation.lazy.items
import com.pocketvalo.app.util.formatMatchDateTime
import androidx.compose.foundation.rememberScrollState
import com.pocketvalo.app.util.formatMatchDateTime
import androidx.compose.foundation.shape.RoundedCornerShape
import com.pocketvalo.app.util.formatMatchDateTime
import androidx.compose.material.icons.Icons
import com.pocketvalo.app.util.formatMatchDateTime
import androidx.compose.material.icons.filled.ArrowBack
import com.pocketvalo.app.util.formatMatchDateTime
import androidx.compose.material3.*
import com.pocketvalo.app.util.formatMatchDateTime
import androidx.compose.runtime.*
import com.pocketvalo.app.util.formatMatchDateTime
import androidx.compose.ui.Alignment
import com.pocketvalo.app.util.formatMatchDateTime
import androidx.compose.ui.Modifier
import com.pocketvalo.app.util.formatMatchDateTime
import androidx.compose.ui.draw.clip
import com.pocketvalo.app.util.formatMatchDateTime
import androidx.compose.ui.graphics.Brush
import com.pocketvalo.app.util.formatMatchDateTime
import androidx.compose.ui.graphics.Color
import com.pocketvalo.app.util.formatMatchDateTime
import androidx.compose.ui.layout.ContentScale
import com.pocketvalo.app.util.formatMatchDateTime
import androidx.compose.ui.text.font.FontWeight
import com.pocketvalo.app.util.formatMatchDateTime
import androidx.compose.ui.text.style.TextAlign
import com.pocketvalo.app.util.formatMatchDateTime
import androidx.compose.ui.text.style.TextOverflow
import com.pocketvalo.app.util.formatMatchDateTime
import androidx.compose.ui.unit.dp
import com.pocketvalo.app.util.formatMatchDateTime
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.pocketvalo.app.data.model.PlayerMatch
import com.pocketvalo.app.data.model.RoundData
import com.pocketvalo.app.ui.viewmodel.PlayerViewModel

private val colorRedTeam  = Color(0xFF3D1A1A)
private val colorBlueTeam = Color(0xFF174D3A)
private val colorSelf     = Color(0xFF3D3000)

@Composable
fun MatchScreen(
    matchId: String,
    navController: NavController,
    playerViewModel: PlayerViewModel
) {
    val uiState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val match = uiState.rawMatchHistory.find { it.metadata.matchId == matchId }
    val mapData = match?.let { uiState.maps[it.metadata.map.uppercase()] }

    val currentName = uiState.accountData?.name ?: ""
    val currentTag  = uiState.accountData?.tag ?: ""

    // Trigger round detail fetch
    LaunchedEffect(matchId) {
        playerViewModel.loadMatchDetail(matchId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
    ) {
        if (match == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Match not found", color = Color(0xFF9BA3AF))
            }
            return
        }

        val redWon     = match.teams?.red?.hasWon ?: false
        val blueWon    = match.teams?.blue?.hasWon ?: false
        val redRounds  = match.teams?.red?.roundsWon ?: 0
        val blueRounds = match.teams?.blue?.roundsWon ?: 0

        val currentPlayer = match.players?.allPlayers?.find {
            it.name.equals(currentName, ignoreCase = true) && it.tag.equals(currentTag, ignoreCase = true)
        }
        val playerOnRed = currentPlayer?.team?.equals("Red", ignoreCase = true) ?: false
        val playerWon   = if (playerOnRed) redWon else blueWon
        val currentPuuid = currentPlayer?.let {
            // puuid not in v3 matchlist — match by name#tag in round stats
            null
        }

        val allPlayers = match.players?.allPlayers
            ?.sortedByDescending { it.stats?.score ?: 0 }
            ?: emptyList()

        LazyColumn(modifier = Modifier.fillMaxSize()) {

            // ── Map header ─────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    if (mapData?.splash != null) {
                        AsyncImage(
                            model = mapData.splash,
                            contentDescription = match.metadata.map,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A2332)))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xFF0F1923))
                                )
                            )
                    )

                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }

                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                        Text(match.metadata.map, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("${match.metadata.mode} · ${match.metadata.region.uppercase()}", color = Color(0xFFFF4655), fontSize = 13.sp)
                        Text(
                            text  = if (match.metadata.gameStartEpoch > 0L)
                                formatMatchDateTime(match.metadata.gameStartEpoch)
                            else match.metadata.gameStartPatched,
                            color = Color(0xFF9BA3AF),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // ── Score block ─────────────────────────────────────────────────
            item {
                val resultText  = if (playerWon) "VICTORY" else "DEFEAT"
                val resultColor = if (playerWon) Color(0xFF4ADE80) else Color(0xFFFF4655)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(resultText, color = resultColor, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                    // Tim player selalu di kiri, lawan di kanan
                    val myRounds  = if (playerOnRed) redRounds  else blueRounds
                    val oppRounds = if (playerOnRed) blueRounds else redRounds
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text       = "$myRounds",
                            color      = if (playerWon) Color(0xFF4ADE80) else Color(0xFFFF4655),
                            fontSize   = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("  –  ", color = Color(0xFF9BA3AF), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text       = "$oppRounds",
                            color      = if (!playerWon) Color(0xFF4ADE80) else Color(0xFFFF4655),
                            fontSize   = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── Scoreboard header ───────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("PLAYER", color = Color(0xFF9BA3AF), fontSize = 10.sp, modifier = Modifier.weight(1f))
                    Text("ACS", color = Color(0xFF9BA3AF), fontSize = 10.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.Center)
                    Text("KDA", color = Color(0xFF9BA3AF), fontSize = 10.sp, modifier = Modifier.width(80.dp), textAlign = TextAlign.Center)
                }
                HorizontalDivider(color = Color(0xFF1A2332))
            }

            // ── Unified player list ─────────────────────────────────────────
            items(allPlayers) { player ->
                val isSelf = player.name.equals(currentName, ignoreCase = true) &&
                        player.tag.equals(currentTag, ignoreCase = true)
                val isRed  = player.team.equals("Red", ignoreCase = true)

                val rowBg = when {
                    isSelf -> colorSelf
                    isRed  -> colorRedTeam
                    else   -> colorBlueTeam
                }
                val teamAccent = when {
                    isSelf -> Color(0xFFFFD700)
                    isRed  -> Color(0xFFFF6B6B)
                    else   -> Color(0xFF1CCC5D)
                }

                PlayerRow(player = player, rowBg = rowBg, teamAccent = teamAccent)
            }

            // ── Round Detail section ────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ROUND DETAIL",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "  ·  ${currentName}#${currentTag}",
                        color = Color(0xFF9BA3AF),
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = Color(0xFF1A2332))
                Spacer(modifier = Modifier.height(8.dp))
            }

            when {
                uiState.isLoadingDetail -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFFFF4655),
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Text("Loading round data...", color = Color(0xFF9BA3AF), fontSize = 13.sp)
                            }
                        }
                    }
                }

                uiState.matchDetail?.rounds != null -> {
                    val rounds = uiState.matchDetail!!.rounds!!

                    // Round timeline header
                    item {
                        RoundTimelineBar(
                            rounds = rounds,
                            playerTeam = currentPlayer?.team ?: "Red",
                            currentPlayerName = "$currentName#$currentTag"
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Per-round stats for current player
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Text("RND", color = Color(0xFF9BA3AF), fontSize = 10.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                            Text("RESULT", color = Color(0xFF9BA3AF), fontSize = 10.sp, modifier = Modifier.width(60.dp), textAlign = TextAlign.Center)
                            Text("K", color = Color(0xFF9BA3AF), fontSize = 10.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                            Text("DMG", color = Color(0xFF9BA3AF), fontSize = 10.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.Center)
                            Text("WEAPON", color = Color(0xFF9BA3AF), fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                            Text("ARMOR", color = Color(0xFF9BA3AF), fontSize = 10.sp, modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
                            Text("CR", color = Color(0xFF9BA3AF), fontSize = 10.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                        }
                        HorizontalDivider(color = Color(0xFF1A2332))
                    }

                    items(rounds.size) { index ->
                        val round = rounds[index]
                        val myStats = round.playerStats?.find { stat ->
                            stat.displayName.equals("$currentName#$currentTag", ignoreCase = true)
                        }
                        val playerWonRound = round.winningTeam.equals(currentPlayer?.team ?: "Red", ignoreCase = true)

                        RoundRow(
                            roundNumber = index + 1,
                            round = round,
                            playerWonRound = playerWonRound,
                            kills = myStats?.kills ?: 0,
                            damage = myStats?.totalDamage ?: 0,
                            weaponName = myStats?.economy?.weapon?.name,
                            armorName = myStats?.economy?.armor?.name,
                            creditsRemaining = myStats?.economy?.remaining ?: 0
                        )
                    }
                }

                else -> {
                    item {
                        Text(
                            text = "Round data unavailable",
                            color = Color(0xFF9BA3AF),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

// ── Round timeline bar ──────────────────────────────────────────────────────
@Composable
fun RoundTimelineBar(
    rounds: List<RoundData>,
    playerTeam: String,
    currentPlayerName: String
) {
    val killsPerRound = rounds.map { round ->
        round.playerStats
            ?.find { it.displayName.equals(currentPlayerName, ignoreCase = true) }
            ?.kills ?: 0
    }
    val maxKills = killsPerRound.maxOrNull()?.coerceAtLeast(1) ?: 1

    // Heights in dp — explicit so layout is predictable
    val killLabelHeight = 12.dp
    val minBarHeight    = 16.dp
    val maxBarHeight    = 56.dp   // increased for more visual difference
    val roundNumHeight  = 12.dp
    val totalHeight     = killLabelHeight + maxBarHeight + roundNumHeight
    val barWidth        = 24.dp
    val barSpacing      = 4.dp
    val separatorWidth  = 3.dp

    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
        Text("ROUND TIMELINE", color = Color(0xFF9BA3AF), fontSize = 10.sp, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(barSpacing),
            verticalAlignment = Alignment.Bottom
        ) {
            rounds.forEachIndexed { index, round ->
                val playerWon = round.winningTeam.equals(playerTeam, ignoreCase = true)
                val color = if (playerWon) Color(0xFF4ADE80) else Color(0xFFFF4655)
                val kills = killsPerRound[index]

                // Proportional bar height — explicit float ratio
                val ratio = if (kills == 0) 0f else kills.toFloat() / maxKills
                val barHeight = minBarHeight + (maxBarHeight - minBarHeight) * ratio

                // Halftime separator
                if (index == 12) {
                    Box(
                        modifier = Modifier
                            .width(separatorWidth)
                            .height(totalHeight)
                            .background(Color(0xFF9BA3AF).copy(alpha = 0.4f))
                    )
                }

                // Fixed total height column — kill label + bar + round num
                // Bar is bottom-aligned by giving top spacer = maxBarHeight - barHeight
                Column(
                    modifier = Modifier
                        .width(barWidth)
                        .height(totalHeight),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top spacer: pushes bar down so bars align at bottom
                    Spacer(modifier = Modifier.height(killLabelHeight))
                    Spacer(modifier = Modifier.height(maxBarHeight - barHeight))

                    // Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barHeight)
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(color.copy(alpha = if (kills > 0) 1f else 0.55f)),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        // Kill count inside top of bar (only if bar is tall enough)
                        if (kills > 0 && barHeight >= 18.dp) {
                            Text(
                                text = "$kills",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        // Spike/defuse icon
                        val endIcon = when {
                            round.bombPlanted && !round.bombDefused -> "💥"
                            round.bombDefused -> "🛡"
                            else -> ""
                        }
                        if (endIcon.isNotEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(endIcon, fontSize = 8.sp)
                            }
                        }
                    }

                    // Round number below bar
                    Box(
                        modifier = Modifier.height(roundNumHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = Color(0xFF9BA3AF),
                            fontSize = 7.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Legend
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF4ADE80)))
                Text("Win", color = Color(0xFF9BA3AF), fontSize = 10.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFFF4655)))
                Text("Loss", color = Color(0xFF9BA3AF), fontSize = 10.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("💥", fontSize = 10.sp)
                Text("Spike", color = Color(0xFF9BA3AF), fontSize = 10.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("🛡", fontSize = 10.sp)
                Text("Defuse", color = Color(0xFF9BA3AF), fontSize = 10.sp)
            }
        }
    }
}

// ── Per-round row ───────────────────────────────────────────────────────────
@Composable
fun RoundRow(
    roundNumber: Int,
    round: RoundData,
    playerWonRound: Boolean,
    kills: Int,
    damage: Int,
    weaponName: String?,
    armorName: String?,
    creditsRemaining: Int
) {
    val bgColor = if (roundNumber % 2 == 0) Color(0xFF141E26) else Color(0xFF0F1923)
    val resultColor = if (playerWonRound) Color(0xFF4ADE80) else Color(0xFFFF4655)

    val armorShort = when {
        armorName == null -> "—"
        armorName.contains("Heavy", ignoreCase = true) -> "Heavy"
        armorName.contains("Light", ignoreCase = true) -> "Light"
        else -> "—"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Round number
        Text(
            text = "$roundNumber",
            color = Color(0xFF9BA3AF),
            fontSize = 12.sp,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.Center
        )

        // Win/Loss
        Text(
            text = if (playerWonRound) "W" else "L",
            color = resultColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(60.dp),
            textAlign = TextAlign.Center
        )

        // Kills
        Text(
            text = if (kills > 0) "$kills" else "—",
            color = if (kills > 0) Color.White else Color(0xFF9BA3AF),
            fontSize = 12.sp,
            fontWeight = if (kills > 0) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.width(24.dp),
            textAlign = TextAlign.Center
        )

        // Damage
        Text(
            text = if (damage > 0) "$damage" else "—",
            color = when {
                damage >= 150 -> Color(0xFF4ADE80)
                damage >= 100 -> Color(0xFFFFD700)
                damage > 0    -> Color.White
                else          -> Color(0xFF9BA3AF)
            },
            fontSize = 12.sp,
            modifier = Modifier.width(44.dp),
            textAlign = TextAlign.Center
        )

        // Weapon
        Text(
            text = weaponName ?: "—",
            color = if (weaponName != null) Color.White else Color(0xFF9BA3AF),
            fontSize = 11.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Armor
        Text(
            text = armorShort,
            color = when (armorShort) {
                "Heavy" -> Color(0xFF60A5FA)
                "Light" -> Color(0xFF9BA3AF)
                else    -> Color(0xFF4B5563)
            },
            fontSize = 11.sp,
            modifier = Modifier.width(52.dp),
            textAlign = TextAlign.Center
        )

        // Credits remaining
        Text(
            text = if (creditsRemaining > 0) "$creditsRemaining" else "—",
            color = Color(0xFFFFD700),
            fontSize = 11.sp,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PlayerRow(
    player: PlayerMatch,
    rowBg: Color,
    teamAccent: Color
) {
    val acs = player.stats?.score ?: 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(rowBg)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val agentUrl = player.assets?.agent?.small
        if (agentUrl != null) {
            AsyncImage(
                model = agentUrl,
                contentDescription = player.character,
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF374151)),
                contentAlignment = Alignment.Center
            ) {
                Text(player.character.firstOrNull()?.toString() ?: "?", color = Color.White, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${player.name}#${player.tag}",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = player.character, color = teamAccent, fontSize = 11.sp)
        }

        Text(
            text = "$acs",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(44.dp),
            textAlign = TextAlign.Center
        )

        Text(
            text = "${player.stats?.kills ?: 0} / ${player.stats?.deaths ?: 0} / ${player.stats?.assists ?: 0}",
            color = Color(0xFF9BA3AF),
            fontSize = 12.sp,
            modifier = Modifier.width(80.dp),
            textAlign = TextAlign.Center
        )
    }
}