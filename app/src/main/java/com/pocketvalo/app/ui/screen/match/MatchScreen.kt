package com.pocketvalo.app.ui.screen.match

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.pocketvalo.app.data.model.PlayerMatch
import com.pocketvalo.app.ui.viewmodel.PlayerViewModel

@Composable
fun MatchScreen(
    matchId: String,
    navController: NavController,
    playerViewModel: PlayerViewModel = viewModel()
) {
    val uiState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val match = uiState.matchHistory.find { it.metadata.matchId == matchId }
    val mapData = match?.let { uiState.maps[it.metadata.map.uppercase()] }

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

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Map image header
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
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF1A2332))
                        )
                    }

                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color(0xFF0F1923)
                                    )
                                )
                            )
                    )

                    // Back button
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    // Match result overlay
                    val redWon = match.teams?.red?.hasWon ?: false
                    val blueWon = match.teams?.blue?.hasWon ?: false
                    val redRounds = match.teams?.red?.roundsWon ?: 0
                    val blueRounds = match.teams?.blue?.roundsWon ?: 0

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = match.metadata.map,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${match.metadata.mode} · ${match.metadata.region.uppercase()}",
                            color = Color(0xFFFF4655),
                            fontSize = 13.sp
                        )
                        Text(
                            text = match.metadata.gameStartPatched,
                            color = Color(0xFF9BA3AF),
                            fontSize = 12.sp
                        )
                    }

                    // Score
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$redRounds",
                            color = if (redWon) Color(0xFF4ADE80) else Color(0xFFFF4655),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " - ",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$blueRounds",
                            color = if (blueWon) Color(0xFF4ADE80) else Color(0xFFFF4655),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Scoreboard header
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PLAYER",
                        color = Color(0xFF9BA3AF),
                        fontSize = 10.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(text = "ACS", color = Color(0xFF9BA3AF), fontSize = 10.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
                    Text(text = "KDA", color = Color(0xFF9BA3AF), fontSize = 10.sp, modifier = Modifier.width(72.dp), textAlign = TextAlign.Center)
                }
                HorizontalDivider(color = Color(0xFF1A2332))
            }

            // Red team
            val redPlayers = match.players?.allPlayers
                ?.filter { it.team == "Red" }
                ?.sortedByDescending { it.stats?.score ?: 0 }
                ?: emptyList()

            item {
                Text(
                    text = "RED TEAM",
                    color = Color(0xFFFF4655),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            items(redPlayers) { player ->
                PlayerRow(player = player, teamColor = Color(0xFFFF4655))
            }

            // Blue team
            val bluePlayers = match.players?.allPlayers
                ?.filter { it.team == "Blue" }
                ?.sortedByDescending { it.stats?.score ?: 0 }
                ?: emptyList()

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "BLUE TEAM",
                    color = Color(0xFF60A5FA),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            items(bluePlayers) { player ->
                PlayerRow(player = player, teamColor = Color(0xFF60A5FA))
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun PlayerRow(player: PlayerMatch, teamColor: Color) {
    val acs = if ((player.stats?.score ?: 0) > 0) player.stats!!.score else 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Agent portrait
        val agentUrl = player.assets?.agent?.small
        if (agentUrl != null) {
            AsyncImage(
                model = agentUrl,
                contentDescription = player.character,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF374151)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = player.character.firstOrNull()?.toString() ?: "?",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Name + agent
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${player.name}#${player.tag}",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = player.character,
                color = teamColor,
                fontSize = 11.sp
            )
        }

        // ACS
        Text(
            text = "$acs",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.Center
        )

        // KDA
        Text(
            text = "${player.stats?.kills ?: 0}/${player.stats?.deaths ?: 0}/${player.stats?.assists ?: 0}",
            color = Color(0xFF9BA3AF),
            fontSize = 13.sp,
            modifier = Modifier.width(72.dp),
            textAlign = TextAlign.Center
        )
    }
}