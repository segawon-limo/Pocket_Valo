package com.pocketvalo.app.ui.screen.loading

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.pocketvalo.app.R
import com.pocketvalo.app.ui.navigation.Screen
import com.pocketvalo.app.ui.viewmodel.LoadingViewModel
import com.pocketvalo.app.ui.viewmodel.PlayerViewModel
import com.pocketvalo.app.ui.viewmodel.StoreViewModel
import com.pocketvalo.app.ui.viewmodel.WeaponsViewModel
import kotlinx.coroutines.delay

private val episodeImages = listOf(
    R.raw.episode1,
    R.raw.episode2,
    R.raw.episode3,
    R.raw.episode4,
    R.raw.episode5,
    R.raw.episode6,
    R.raw.episode7,
    R.raw.episode8,
    R.raw.episode9,
    R.raw.episode10,
)

@Composable
fun LoadingScreen(
    navController: NavController,
    playerViewModel: PlayerViewModel,
    storeViewModel: StoreViewModel,
    weaponsViewModel: WeaponsViewModel,
    loadingViewModel: LoadingViewModel = viewModel()
) {
    val uiState by loadingViewModel.uiState.collectAsState()

    val animatedProgress by animateFloatAsState(
        targetValue   = uiState.progress,
        animationSpec = tween(durationMillis = 300),
        label         = "loading_progress"
    )

    // Shuffle list lalu iterate — tidak repeat sampai semua habis, lalu shuffle ulang
    val shuffledImages = remember { episodeImages.shuffled().toMutableList() }
    var imageIndex      by remember { mutableIntStateOf(0) }
    var currentImageRes by remember { mutableIntStateOf(shuffledImages[0]) }
    var imageAlpha      by remember { mutableFloatStateOf(0f) }
    val animatedAlpha   by animateFloatAsState(
        targetValue   = imageAlpha,
        animationSpec = tween(durationMillis = 600),
        label         = "image_alpha"
    )

    LaunchedEffect(Unit) {
        loadingViewModel.startPrefetch(playerViewModel, weaponsViewModel)
    }

    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) {
            // Reset session expired state dan load store setelah login pertama
            // StoreViewModel skip loadStore() di init kalau belum login —
            // ini trigger-nya setelah token sudah tersedia
            storeViewModel.resetSessionExpired()
            storeViewModel.loadStore(forceRefresh = false)
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Loading.route) { inclusive = true }
            }
        }
    }

    // Rotate image every 3 seconds with crossfade, no repeats within a cycle
    LaunchedEffect(Unit) {
        currentImageRes = shuffledImages[0]
        imageAlpha = 1f
        while (true) {
            delay(3_000L)
            // Fade out
            imageAlpha = 0f
            delay(600L)
            // Advance index — reshuffle when cycle completes
            imageIndex++
            if (imageIndex >= shuffledImages.size) {
                imageIndex = 0
                val newShuffle = episodeImages.shuffled()
                shuffledImages.clear()
                shuffledImages.addAll(newShuffle)
            }
            currentImageRes = shuffledImages[imageIndex]
            // Fade in
            imageAlpha = 1f
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Background episode image ──────────────────────────────────────────
        AsyncImage(
            model              = currentImageRes,
            contentDescription = null,
            modifier           = Modifier
                .fillMaxSize()
                .alpha(animatedAlpha),
            contentScale       = ContentScale.Crop
        )

        // ── Dark overlay — full screen ────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F1923).copy(alpha = 0.55f))
        )

        // ── Gradient fade dari bawah ke atas — tutup area progress bar ────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.4f to Color(0xFF0F1923).copy(alpha = 0.85f),
                            1.0f to Color(0xFF0F1923)
                        )
                    )
                )
        )

        // ── Progress bar + label — bottom ─────────────────────────────────────
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text          = uiState.stepLabel,
                color         = Color(0xFF9BA3AF),
                fontSize      = 13.sp,
                textAlign     = TextAlign.Center,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress   = { animatedProgress },
                modifier   = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color      = Color(0xFFFF4655),
                trackColor = Color(0xFF1A2332).copy(alpha = 0.6f),
                strokeCap  = StrokeCap.Round
            )
        }
    }
}