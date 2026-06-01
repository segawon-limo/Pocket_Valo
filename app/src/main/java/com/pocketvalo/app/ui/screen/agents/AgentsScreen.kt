package com.pocketvalo.app.ui.screen.agents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.pocketvalo.app.data.model.AgentData
import com.pocketvalo.app.ui.navigation.Screen
import com.pocketvalo.app.ui.viewmodel.AgentsViewModel

@Composable
fun AgentsScreen(
    navController: NavController,
    agentsViewModel: AgentsViewModel
) {
    val uiState by agentsViewModel.uiState.collectAsStateWithLifecycle()

    // Map role name → icon URL (ambil dari agents data)
    val roleIconMap = remember(uiState.agents) {
        uiState.agents
            .mapNotNull { it.role }
            .distinctBy { it.displayName }
            .sortedBy { it.displayName }
            .associate { it.displayName to it.displayIcon }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
    ) {
        // ── Header: title kiri, role icons kanan ─────────────────────────────
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text       = "AGENTS",
                color      = Color.White,
                fontSize   = 24.sp,
                fontWeight = FontWeight.Bold
            )

            // Role icon filters — sejajar dengan title
            if (roleIconMap.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    roleIconMap.entries.forEach { (roleName, iconUrl) ->
                        val isSelected = uiState.selectedRole == roleName
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(
                                    if (isSelected) Color(0xFFFF4655).copy(alpha = 0.2f)
                                    else Color(0xFF1A2332)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) Color(0xFFFF4655) else Color.Transparent,
                                    shape = androidx.compose.foundation.shape.CircleShape
                                )
                                .clickable {
                                    // Toggle: klik role yang sama → show all
                                    if (uiState.selectedRole == roleName) {
                                        agentsViewModel.filterByRole(null)
                                    } else {
                                        agentsViewModel.filterByRole(roleName)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (iconUrl != null) {
                                AsyncImage(
                                    model              = iconUrl,
                                    contentDescription = roleName,
                                    modifier           = Modifier
                                        .size(20.dp)
                                        .then(
                                            if (isSelected) Modifier
                                            else Modifier.alpha(0.5f)
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFFF4655))
                }
            }
            uiState.error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.error!!, color = Color(0xFFFF4655))
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns             = GridCells.Fixed(2),
                    modifier            = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement   = Arrangement.spacedBy(12.dp),
                    contentPadding        = PaddingValues(bottom = 16.dp)
                ) {
                    items(uiState.filteredAgents) { agent ->
                        AgentCard(
                            agent   = agent,
                            onClick = { navController.navigate(Screen.AgentDetail.createRoute(agent.uuid)) }
                        )
                    }
                }
            }
        }
    }
}

// ── Helper: parse hex string ke Color ────────────────────────────────────────

internal fun parseGradientColors(hexList: List<String>?): List<Color>? {
    if (hexList.isNullOrEmpty()) return null
    return hexList.mapNotNull { hex ->
        try {
            // API return 8-char RRGGBBAA, Color.parseColor butuh #AARRGGBB
            val clean = hex.trimStart('#')
            val color = if (clean.length == 8) {
                // RRGGBBAA → parse manual
                val r = clean.substring(0, 2).toInt(16)
                val g = clean.substring(2, 4).toInt(16)
                val b = clean.substring(4, 6).toInt(16)
                Color(r / 255f, g / 255f, b / 255f, 1f)
            } else {
                Color(android.graphics.Color.parseColor("#$clean"))
            }
            color
        } catch (e: Exception) { null }
    }.takeIf { it.size >= 2 }
}

// ── Agent card ────────────────────────────────────────────────────────────────

@Composable
fun AgentCard(agent: AgentData, onClick: () -> Unit) {
    val gradientColors = parseGradientColors(agent.backgroundGradientColors)

    // Horizontal gradient kiri → kanan (2 warna pertama dari API)
    val cardBrush = if (gradientColors != null) {
        Brush.horizontalGradient(
            colors = listOf(gradientColors[0], gradientColors[1])
        )
    } else {
        Brush.horizontalGradient(listOf(Color(0xFF1A2332), Color(0xFF0F1923)))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(cardBrush)
            .clickable { onClick() }
    ) {
        // Background texture dari API (di belakang portrait)
        if (agent.background != null) {
            AsyncImage(
                model              = agent.background,
                contentDescription = null,
                modifier           = Modifier.fillMaxSize(),
                contentScale       = ContentScale.Crop,
                alpha              = 0.25f
            )
        }

        // Full portrait
        if (agent.fullPortrait != null) {
            AsyncImage(
                model              = agent.fullPortrait,
                contentDescription = agent.displayName,
                modifier           = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                contentScale       = ContentScale.Fit
            )
        }

        // Gradient overlay bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))
                )
        )

        // Role + name
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
        ) {
            agent.role?.let { role ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (role.displayIcon != null) {
                        AsyncImage(
                            model              = role.displayIcon,
                            contentDescription = role.displayName,
                            modifier           = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text  = role.displayName.uppercase(),
                        color = Color(0xFFE0E0E0).copy(alpha = 0.8f),
                        fontSize = 10.sp
                    )
                }
            }
            Text(
                text       = agent.displayName,
                color      = Color.White,
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}