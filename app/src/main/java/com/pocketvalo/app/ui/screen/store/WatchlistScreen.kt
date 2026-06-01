package com.pocketvalo.app.ui.screen.store

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pocketvalo.app.data.local.entity.WatchlistEntity
import com.pocketvalo.app.ui.viewmodel.BrowseSkinItem
import com.pocketvalo.app.ui.viewmodel.MAX_WATCHLIST
import com.pocketvalo.app.ui.viewmodel.WatchlistViewModel
import kotlinx.coroutines.launch

// Index di LazyColumn tempat browse section dimulai
// Slots (1) + divider (1) + filter (1) = item ke-3 (index 3)
private const val BROWSE_SECTION_INDEX = 3

@Composable
fun WatchlistScreen(
    viewModel: WatchlistViewModel,
    onBack: () -> Unit
) {
    val uiState     by viewModel.uiState.collectAsState()
    val listState   = rememberLazyListState()
    val scope       = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    // Filter skins berdasarkan search query
    val displayedSkins = remember(uiState.filteredSkins, searchQuery) {
        if (searchQuery.isBlank()) uiState.filteredSkins
        else uiState.filteredSkins.filter {
            it.displayName.contains(searchQuery, ignoreCase = true)
        }
    }

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied */ }

    // Scroll ke browse section
    fun scrollToBrowse() {
        scope.launch {
            listState.animateScrollToItem(BROWSE_SECTION_INDEX)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
    ) {
        LazyColumn(
            state          = listState,
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F1923))
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Column(
                        modifier            = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Watchlist", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text     = "${uiState.watchlist.size}/$MAX_WATCHLIST slots",
                            color    = Color(0xFF6B7280),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // ── 3 Slot watchlist ──────────────────────────────────────────────
            item {
                Column(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    repeat(MAX_WATCHLIST) { index ->
                        val item = uiState.watchlist.getOrNull(index)
                        WatchlistSlot(
                            item       = item,
                            onRemove   = { viewModel.removeFromWatchlist(it) },
                            onTapEmpty = { scrollToBrowse() }
                        )
                    }
                }
            }

            // ── Divider + Browse title ────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text          = "BROWSE SKINS",
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
                                    listOf(Color(0xFFFF4655).copy(alpha = 0.4f), Color.Transparent)
                                )
                            )
                    )
                }
            }

            // ── Search + Filter ───────────────────────────────────────────────
            item {
                Column(
                    modifier            = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Search bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1A2332))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Search,
                            contentDescription = null,
                            tint               = Color(0xFF6B7280),
                            modifier           = Modifier.size(18.dp)
                        )
                        BasicTextField(
                            value         = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier      = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            textStyle     = TextStyle(color = Color.White, fontSize = 14.sp),
                            cursorBrush   = SolidColor(Color(0xFFFF4655)),
                            singleLine    = true,
                            decorationBox = { inner ->
                                if (searchQuery.isEmpty()) {
                                    Text("Search skin...", color = Color(0xFF4A5568), fontSize = 14.sp)
                                }
                                inner()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector        = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint               = Color(0xFF6B7280),
                                modifier           = Modifier
                                    .size(18.dp)
                                    .clickable { searchQuery = "" }
                            )
                        }
                    }

                    // Weapon filter chips
                    LazyRow(
                        contentPadding        = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            WatchlistFilterChip(
                                label      = "All",
                                isSelected = uiState.selectedFilter == null,
                                onClick    = { viewModel.filterByWeapon(null) }
                            )
                        }
                        items(viewModel.weaponFilters) { type ->
                            WatchlistFilterChip(
                                label      = type,
                                isSelected = uiState.selectedFilter == type,
                                onClick    = { viewModel.filterByWeapon(type) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Skin grid (2 kolom) ───────────────────────────────────────────
            if (uiState.isLoadingSkins) {
                item {
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFFF4655))
                    }
                }
            } else if (displayedSkins.isEmpty()) {
                item {
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text      = if (searchQuery.isBlank()) "No skins found" else "No results for \"$searchQuery\"",
                            color     = Color(0xFF6B7280),
                            fontSize  = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val rows = displayedSkins.chunked(2)
                items(rows) { row ->
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { skin ->
                            BrowseSkinCard(
                                skin      = skin,
                                isFull    = uiState.isFull,
                                viewModel = viewModel,
                                modifier  = Modifier.weight(1f),
                                onAdd     = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    viewModel.addToWatchlist(skin)
                                }
                            )
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

// ── Watchlist slot ────────────────────────────────────────────────────────────

@Composable
private fun WatchlistSlot(
    item: WatchlistEntity?,
    onRemove: (WatchlistEntity) -> Unit,
    onTapEmpty: () -> Unit
) {
    if (item != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1A2332))
                .padding(12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F1923))
            ) {
                if (item.iconUrl != null) {
                    AsyncImage(
                        model              = item.iconUrl,
                        contentDescription = item.displayName,
                        modifier           = Modifier.fillMaxSize().padding(6.dp),
                        contentScale       = ContentScale.Fit
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = item.displayName,
                    color      = Color.White,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Text(text = item.weaponType ?: "", color = Color(0xFF6B7280), fontSize = 12.sp)
            }
            IconButton(onClick = { onRemove(item) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFF4A5568), modifier = Modifier.size(18.dp))
            }
        }
    } else {
        // Empty slot — tap to scroll to browse
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(listOf(Color(0xFF2A3A4A), Color(0xFF1A2332))),
                    shape = RoundedCornerShape(12.dp)
                )
                .background(Color(0xFF0F1923))
                .clickable { onTapEmpty() },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF3A4A5C), modifier = Modifier.size(18.dp))
                Text("Tap to add a skin", color = Color(0xFF3A4A5C), fontSize = 13.sp)
            }
        }
    }
}

// ── Browse skin card ──────────────────────────────────────────────────────────

@Composable
private fun BrowseSkinCard(
    skin: BrowseSkinItem,
    isFull: Boolean,
    viewModel: WatchlistViewModel,
    modifier: Modifier = Modifier,
    onAdd: () -> Unit
) {
    var inWatchlist by remember { mutableStateOf(false) }

    LaunchedEffect(skin.skinUuid) {
        inWatchlist = viewModel.isInWatchlist(skin.skinUuid)
    }

    val isDisabled = isFull && !inWatchlist

    Box(
        modifier = modifier
            .height(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (inWatchlist) Color(0xFF1E3A2E) else Color(0xFF1A2332))
            .then(
                if (inWatchlist) Modifier.border(1.dp, Color(0xFF4ADE80).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                else Modifier
            )
            .clickable(enabled = !isDisabled) {
                if (inWatchlist) {
                    viewModel.removeFromWatchlistBySkinUuid(skin.skinUuid)
                } else {
                    onAdd()
                }
                inWatchlist = !inWatchlist
            }
    ) {
        Column(
            modifier            = Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            AsyncImage(
                model              = skin.iconUrl,
                contentDescription = skin.displayName,
                modifier           = Modifier.fillMaxWidth().weight(1f),
                contentScale       = ContentScale.Fit
            )
            Text(
                text      = skin.displayName,
                color     = if (isDisabled) Color(0xFF4A5568) else Color.White,
                fontSize  = 10.sp,
                maxLines  = 1,
                overflow  = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth()
            )
        }

        if (inWatchlist) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(18.dp)
                    .background(Color(0xFF4ADE80), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", color = Color.Black, fontSize = 10.sp)
            }
        }

        if (isDisabled) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F1923).copy(alpha = 0.5f)))
        }
    }
}

// ── Filter chip ───────────────────────────────────────────────────────────────

@Composable
private fun WatchlistFilterChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) Color(0xFFFF4655) else Color(0xFF1A2332))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text       = label,
            color      = if (isSelected) Color.White else Color(0xFF9BA3AF),
            fontSize   = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}