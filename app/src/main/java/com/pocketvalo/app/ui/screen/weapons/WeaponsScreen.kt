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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.pocketvalo.app.data.model.WeaponData
import com.pocketvalo.app.ui.viewmodel.WeaponsViewModel

@Composable
fun WeaponsScreen(
    weaponsViewModel: WeaponsViewModel
) {
    val uiState by weaponsViewModel.uiState.collectAsStateWithLifecycle()
    val categoryOrder = weaponsViewModel.categoryOrder

    // Build tab list: only categories that actually exist in data, in defined order
    val availableCategories = remember(uiState.weapons) {
        val existing = uiState.weapons
            .map { it.category.substringAfterLast("::") }
            .toSet()
        categoryOrder.filter { it in existing }
    }

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

        if (availableCategories.isNotEmpty()) {
            val selectedIndex = if (uiState.selectedCategory == null) 0
            else availableCategories.indexOf(uiState.selectedCategory) + 1

            ScrollableTabRow(
                selectedTabIndex = selectedIndex.coerceAtLeast(0),
                containerColor = Color(0xFF0F1923),
                contentColor = Color(0xFFFF4655),
                edgePadding = 16.dp,
                divider = {}
            ) {
                // "All" tab
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
                availableCategories.forEach { category ->
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
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(uiState.filteredWeapons) { weapon ->
                        WeaponCard(
                            weapon        = weapon,
                            equippedSkins = uiState.equippedSkins
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeaponCard(weapon: WeaponData, equippedSkins: Map<String, String> = emptyMap()) {
    // Cari skin yang sedang dipakai — match by skinUuid dari loadout
    val equippedSkinUuid = equippedSkins[weapon.uuid]
    val equippedSkin     = weapon.skins.firstOrNull { it.uuid == equippedSkinUuid }
    // Ambil displayIcon dari skin yang equipped — fallback ke default weapon icon
    val equippedIcon     = equippedSkin?.levels?.firstOrNull { it.displayIcon != null }?.displayIcon
        ?: equippedSkin?.chromas?.firstOrNull()?.fullRender
    val displayIcon      = equippedIcon ?: weapon.displayIcon
    val category = weapon.category.substringAfterLast("::")
    val cost = weapon.shopData?.cost

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2332)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // Weapon image area with subtle gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF0F1923), Color(0xFF1A2332))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (displayIcon != null) {
                    AsyncImage(
                        model              = displayIcon,
                        contentDescription = weapon.displayName,
                        modifier           = Modifier
                            .fillMaxWidth(0.85f)
                            .height(80.dp),
                        contentScale       = ContentScale.Fit
                    )
                }
            }

            // Divider
            HorizontalDivider(
                color = Color(0xFF0F1923),
                thickness = 1.dp
            )

            // Weapon info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text       = weapon.displayName,
                            color      = Color.White,
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text  = category,
                            color = Color(0xFFFF4655),
                            fontSize = 12.sp
                        )
                        // Nama skin yang sedang dipakai
                        if (equippedSkin != null) {
                            Text(
                                text     = equippedSkin.displayName,
                                color    = Color(0xFF9BA3AF),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Credits badge
                    if (cost != null && cost > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0F1923))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "◈ $cost",
                                color = Color(0xFFFFD700),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                weapon.weaponStats?.let { stats ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        stats.fireRate?.let {
                            StatChip(
                                label = "Fire Rate",
                                value = "${it.toInt()}/s",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        stats.magazineSize?.let {
                            StatChip(
                                label = "Magazine",
                                value = "$it",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        stats.reloadTimeSeconds?.let {
                            StatChip(
                                label = "Reload",
                                value = "${String.format("%.1f", it)}s",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    stats.damageRanges?.firstOrNull()?.let { range ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatChip(label = "Head", value = "${range.headDamage.toInt()}", modifier = Modifier.weight(1f))
                            StatChip(label = "Body", value = "${range.bodyDamage.toInt()}", modifier = Modifier.weight(1f))
                            StatChip(label = "Leg", value = "${range.legDamage.toInt()}", modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0F1923))
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = label,
            color = Color(0xFF9BA3AF),
            fontSize = 10.sp
        )
    }
}