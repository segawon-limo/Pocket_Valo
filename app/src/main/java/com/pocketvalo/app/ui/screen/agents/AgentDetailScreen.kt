package com.pocketvalo.app.ui.screen.agents

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.pocketvalo.app.data.model.AgentAbility
import com.pocketvalo.app.data.model.AgentData
import com.pocketvalo.app.ui.viewmodel.AgentsViewModel

@Composable
fun AgentDetailScreen(
    agentId: String,
    navController: NavController,
    agentsViewModel: AgentsViewModel
) {
    val uiState by agentsViewModel.uiState.collectAsStateWithLifecycle()
    val agent   = uiState.agents.find { it.uuid == agentId }

    if (agent == null) {
        Box(
            modifier         = Modifier.fillMaxSize().background(Color(0xFF0F1923)),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator(color = Color(0xFFFF4655)) }
        return
    }

    val gradientColors = parseGradientColors(agent.backgroundGradientColors)

    // Header: horizontal gradient kiri → kanan
    val headerBrush = if (gradientColors != null) {
        Brush.horizontalGradient(listOf(gradientColors[0], gradientColors[1]))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFF1A2332), Color(0xFF0F1923)))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .background(headerBrush)
            ) {
                // Background texture (low opacity, di belakang portrait)
                if (agent.background != null) {
                    AsyncImage(
                        model              = agent.background,
                        contentDescription = null,
                        modifier           = Modifier.fillMaxSize(),
                        contentScale       = ContentScale.Crop,
                        alpha              = 0.2f
                    )
                }

                // Full portrait — lebih besar di detail screen
                if (agent.fullPortrait != null) {
                    AsyncImage(
                        model              = agent.fullPortrait,
                        contentDescription = agent.displayName,
                        modifier           = Modifier
                            .fillMaxHeight()
                            .align(Alignment.BottomCenter),
                        contentScale       = ContentScale.Fit
                    )
                }

                // Gradient fade ke background bawah
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xFF0F1923))
                            )
                        )
                )

                // Back button
                IconButton(
                    onClick  = { navController.popBackStack() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint               = Color.White
                    )
                }

                // Role + nama
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    agent.role?.let { role ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (role.displayIcon != null) {
                                AsyncImage(
                                    model              = role.displayIcon,
                                    contentDescription = role.displayName,
                                    modifier           = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text     = role.displayName.uppercase(),
                                color    = Color.White.copy(alpha = 0.75f),
                                fontSize = 12.sp
                            )
                        }
                    }
                    Text(
                        text       = agent.displayName,
                        color      = Color.White,
                        fontSize   = 36.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // ── Description ───────────────────────────────────────────────────────
        item {
            Text(
                text     = agent.description,
                color    = Color(0xFF9BA3AF),
                fontSize = 14.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            HorizontalDivider(
                color    = Color(0xFF1A2332),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // ── Abilities header ──────────────────────────────────────────────────
        item {
            Text(
                text       = "ABILITIES",
                color      = Color.White,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        val abilities = agent.abilities?.filter { it.displayIcon != null } ?: emptyList()
        items(abilities) { ability ->
            AbilityCard(ability = ability)
            Spacer(modifier = Modifier.height(8.dp))
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun AbilityCard(ability: AgentAbility) {
    val isUltimate = ability.slot == "Ultimate"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isUltimate) Color(0xFF0A524A) else Color(0xFF1A2332))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier         = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0F1923)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model              = ability.displayIcon,
                contentDescription = ability.displayName,
                modifier           = Modifier.size(36.dp).padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = ability.displayName,
                    color      = Color.White,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (isUltimate) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text       = "ULTIMATE",
                        color      = Color(0xFF48E3B9),
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF198072))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text       = ability.description,
                color      = Color(0xFF9BA3AF),
                fontSize   = 13.sp,
                lineHeight = 20.sp
            )
        }
    }
}