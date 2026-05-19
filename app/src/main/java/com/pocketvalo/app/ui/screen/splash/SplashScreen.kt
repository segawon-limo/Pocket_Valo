package com.pocketvalo.app.ui.screen.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.pocketvalo.app.R
import com.pocketvalo.app.ui.navigation.Screen
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current

    var cardImageUrl by remember { mutableStateOf<String?>(null) }
    var imageAlpha by remember { mutableStateOf(0f) }
    var imageReady by remember { mutableStateOf(false) }

    // Animated shimmer gradient
    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by shimmerTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    val imageAlphaAnim by animateFloatAsState(
        targetValue = imageAlpha,
        animationSpec = tween(durationMillis = 700),
        label = "image_fade"
    )

    // Navigate 1500ms after image loads successfully
    LaunchedEffect(imageReady) {
        if (!imageReady) return@LaunchedEffect
        delay(1500L)
        navController.navigate(Screen.Input.route) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    // Set URL, then fallback navigate after 4s if image never loads
    LaunchedEffect(Unit) {
        try {
            val uuids = context.resources.openRawResource(R.raw.player_card_uuids)
                .bufferedReader()
                .readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }

            if (uuids.isNotEmpty()) {
                cardImageUrl = "https://media.valorant-api.com/playercards/${uuids.random()}/largeart.png"
            }
        } catch (_: Exception) { }

        // Fallback: navigate even if image fails or is too slow
        delay(4000L)
        navController.navigate(Screen.Input.route) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
    ) {
        // ── Animated shimmer background (always visible as base) ────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0F1923),
                            Color(0xFF1A1F2E),
                            Color(0xFF2D1B1B),
                            Color(0xFF1A1F2E),
                            Color(0xFF0F1923),
                        ),
                        start = Offset(shimmerOffset - 500f, 0f),
                        end = Offset(shimmerOffset + 500f, 1000f)
                    )
                )
        )

        // ── Player card image — fades in when Coil finishes loading ─────────
        if (cardImageUrl != null) {
            AsyncImage(
                model = cardImageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(imageAlphaAnim),
                contentScale = ContentScale.Crop,
                onSuccess = {
                    imageAlpha = 1f
                    imageReady = true
                }
            )
        }

        // ── Gradient overlay — bottom heavy ─────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xFF0F1923).copy(alpha = 0.2f),
                            0.45f to Color(0xFF0F1923).copy(alpha = 0.4f),
                            1.0f to Color(0xFF0F1923).copy(alpha = 0.95f)
                        )
                    )
                )
        )

        // ── Logo ─────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "POCKET",
                color = Color(0xFFFF4655),
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 8.sp
            )
            Text(
                text = "VALO",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 8.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Your Valorant Companion",
                color = Color(0xFF9BA3AF),
                fontSize = 13.sp,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}