package com.pocketvalo.app.ui.screen.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ShimmerBrush(): Brush {
    val shimmerColors = listOf(
        Color(0xFF1A2332),
        Color(0xFF243040),
        Color(0xFF1A2332)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    height: Int = 16,
    width: Float = 1f
) {
    val brush = ShimmerBrush()
    Box(
        modifier = modifier
            .fillMaxWidth(width)
            .height(height.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(brush)
    )
}

@Composable
fun HomeScreenSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
            .padding(16.dp)
    ) {
        // Player name skeleton
        SkeletonBox(height = 14, width = 0.3f)
        Spacer(modifier = Modifier.height(8.dp))
        SkeletonBox(height = 28, width = 0.6f)
        Spacer(modifier = Modifier.height(8.dp))
        SkeletonBox(height = 14, width = 0.4f)

        Spacer(modifier = Modifier.height(24.dp))

        // Section title
        SkeletonBox(height = 18, width = 0.4f)
        Spacer(modifier = Modifier.height(12.dp))

        // Match cards skeleton
        repeat(5) {
            MatchCardSkeleton()
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun MatchCardSkeleton() {
    val brush = ShimmerBrush()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(brush)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SkeletonBox(height = 12, width = 0.2f)
                SkeletonBox(height = 12, width = 0.3f)
            }
            Spacer(modifier = Modifier.height(8.dp))
            SkeletonBox(height = 16, width = 0.4f)
        }
    }
}