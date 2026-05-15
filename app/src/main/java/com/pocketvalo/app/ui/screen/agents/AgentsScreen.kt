package com.pocketvalo.app.ui.screen.agents

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.pocketvalo.app.data.model.AgentData
import com.pocketvalo.app.ui.viewmodel.AgentsViewModel
import androidx.navigation.NavController
import com.pocketvalo.app.ui.navigation.Screen

@Composable
fun AgentsScreen(
    navController: NavController,
    agentsViewModel: AgentsViewModel = viewModel()
) {
    val uiState by agentsViewModel.uiState.collectAsStateWithLifecycle()

    val roles = uiState.agents
        .mapNotNull { it.role?.displayName }
        .distinct()
        .sorted()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
    ) {
        // Header
        Text(
            text = "AGENTS",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )

        // Role filter
        if (roles.isNotEmpty()) {
            ScrollableTabRow(
                selectedTabIndex = roles.indexOf(uiState.selectedRole).coerceAtLeast(0),
                containerColor = Color(0xFF0F1923),
                contentColor = Color(0xFFFF4655),
                edgePadding = 16.dp,
                divider = {}
            ) {
                // All tab
                Tab(
                    selected = uiState.selectedRole == null,
                    onClick = { agentsViewModel.filterByRole(null) },
                    text = {
                        Text(
                            text = "All",
                            color = if (uiState.selectedRole == null) Color(0xFFFF4655) else Color(0xFF9BA3AF),
                            fontWeight = if (uiState.selectedRole == null) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                roles.forEach { role ->
                    Tab(
                        selected = uiState.selectedRole == role,
                        onClick = { agentsViewModel.filterByRole(role) },
                        text = {
                            Text(
                                text = role,
                                color = if (uiState.selectedRole == role) Color(0xFFFF4655) else Color(0xFF9BA3AF),
                                fontWeight = if (uiState.selectedRole == role) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
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
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(uiState.filteredAgents) { agent ->
                        AgentCard(
                            agent = agent,
                            onClick = {
                                navController.navigate(Screen.AgentDetail.createRoute(agent.uuid))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AgentCard(agent: AgentData, onClick: () -> Unit) {
    val gradientColors = agent.backgroundGradientColors
        ?.take(2)
        ?.mapNotNull { hex ->
            try {
                Color(android.graphics.Color.parseColor("#$hex"))
            } catch (e: Exception) { null }
        }

    val cardBrush = if (gradientColors?.size == 2) {
        Brush.verticalGradient(gradientColors)
    } else {
        Brush.verticalGradient(listOf(Color(0xFF1A2332), Color(0xFF0F1923)))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(cardBrush)
            .clickable { onClick() }
    ) {
        // Agent full portrait
        if (agent.fullPortrait != null) {
            AsyncImage(
                model = agent.fullPortrait,
                contentDescription = agent.displayName,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                contentScale = ContentScale.Fit
            )
        }

        // Gradient overlay bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xCC000000))
                    )
                )
        )

        // Role icon + name
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
        ) {
            agent.role?.let { role ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (role.displayIcon != null) {
                        AsyncImage(
                            model = role.displayIcon,
                            contentDescription = role.displayName,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = role.displayName.uppercase(),
                        color = Color(0xFF9BA3AF),
                        fontSize = 10.sp
                    )
                }
            }
            Text(
                text = agent.displayName,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}