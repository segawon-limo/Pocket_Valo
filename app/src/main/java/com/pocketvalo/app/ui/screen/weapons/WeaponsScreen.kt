package com.pocketvalo.app.ui.screen.weapons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.pocketvalo.app.data.model.WeaponData
import com.pocketvalo.app.ui.viewmodel.WeaponsViewModel

@Composable
fun WeaponsScreen(
    weaponsViewModel: WeaponsViewModel = viewModel()
) {
    val uiState by weaponsViewModel.uiState.collectAsStateWithLifecycle()

    val categories = uiState.weapons
        .map { it.category.substringAfterLast("::") }
        .distinct()
        .sorted()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
    ) {
        Text(
            text = "WEAPONS",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )

        // Category filter
        if (categories.isNotEmpty()) {
            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(
                    uiState.selectedCategory
                ).coerceAtLeast(0),
                containerColor = Color(0xFF0F1923),
                contentColor = Color(0xFFFF4655),
                edgePadding = 16.dp,
                divider = {}
            ) {
                Tab(
                    selected = uiState.selectedCategory == null,
                    onClick = { weaponsViewModel.filterByCategory(null) },
                    text = {
                        Text(
                            text = "All",
                            color = if (uiState.selectedCategory == null) Color(0xFFFF4655) else Color(0xFF9BA3AF),
                            fontWeight = if (uiState.selectedCategory == null) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                categories.forEach { category ->
                    Tab(
                        selected = uiState.selectedCategory == category,
                        onClick = { weaponsViewModel.filterByCategory(category) },
                        text = {
                            Text(
                                text = category,
                                color = if (uiState.selectedCategory == category) Color(0xFFFF4655) else Color(0xFF9BA3AF),
                                fontWeight = if (uiState.selectedCategory == category) FontWeight.Bold else FontWeight.Normal
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(uiState.filteredWeapons) { weapon ->
                        WeaponCard(weapon = weapon)
                    }
                }
            }
        }
    }
}

@Composable
fun WeaponCard(weapon: WeaponData) {
    val category = weapon.category.substringAfterLast("::")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2332)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Weapon image
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                if (weapon.displayIcon != null) {
                    AsyncImage(
                        model = weapon.displayIcon,
                        contentDescription = weapon.displayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Weapon info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = weapon.displayName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = category,
                    color = Color(0xFFFF4655),
                    fontSize = 12.sp
                )

                weapon.weaponStats?.let { stats ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        stats.fireRate?.let {
                            StatChip(label = "Fire Rate", value = "${it.toInt()}/s")
                        }
                        stats.magazineSize?.let {
                            StatChip(label = "Mag", value = "$it")
                        }
                    }
                    stats.damageRanges?.firstOrNull()?.let { range ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatChip(label = "Head", value = "${range.headDamage.toInt()}")
                            StatChip(label = "Body", value = "${range.bodyDamage.toInt()}")
                            StatChip(label = "Leg", value = "${range.legDamage.toInt()}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = label,
            color = Color(0xFF9BA3AF),
            fontSize = 10.sp
        )
    }
}