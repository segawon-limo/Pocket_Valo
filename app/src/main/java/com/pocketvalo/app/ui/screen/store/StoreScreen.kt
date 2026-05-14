package com.pocketvalo.app.ui.screen.store

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun StoreScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Store\nComing Soon",
            color = Color(0xFF9BA3AF),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}