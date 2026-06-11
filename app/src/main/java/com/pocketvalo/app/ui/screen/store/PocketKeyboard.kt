package com.pocketvalo.app.ui.screen.store

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ── Colour tokens ─────────────────────────────────────────────────────────────
private val BG          = Color(0xFF0D1520)
private val KEY_DEFAULT = Color(0xFF1E2D3D)
private val KEY_ACCENT  = Color(0xFF243447)
private val KEY_ACTION  = Color(0xFFFF4655)
private val KEY_SPECIAL = Color(0xFF162030)
private val KEY_TEXT    = Color(0xFFE8EDF2)
private val KEY_SUB     = Color(0xFF6B7F8F)
private val DIVIDER     = Color(0xFF0A1118)

// ── Key layout rows ───────────────────────────────────────────────────────────
private val ROW_NUMBERS  = listOf("1","2","3","4","5","6","7","8","9","0")
private val ROW_SPECIALS = listOf("!","@","#","$","%","^","&","*","(",")")
private val ROW_1        = listOf("q","w","e","r","t","y","u","i","o","p")
private val ROW_2        = listOf("a","s","d","f","g","h","j","k","l")
private val ROW_3        = listOf("z","x","c","v","b","n","m")
private val ROW_EXTRA    = listOf("-","_",".",",","@")

enum class KeyboardMode { LOWER, UPPER, SYMBOLS }

// ── Single key ────────────────────────────────────────────────────────────────
@Composable
private fun Key(
    label: String,
    modifier: Modifier = Modifier,
    bgColor: Color = KEY_DEFAULT,
    textColor: Color = KEY_TEXT,
    fontSize: Int = 15,
    fontWeight: FontWeight = FontWeight.Medium,
    subLabel: String? = null,
    onClick: () -> Unit
) {
    val haptic      = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed     by interaction.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "keyScale"
    )
    val bgAnimated by animateColorAsState(
        targetValue = if (pressed) bgColor.copy(alpha = 0.6f) else bgColor,
        label = "keyBg"
    )

    Box(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(bgAnimated)
            .clickable(
                interactionSource = interaction,
                indication        = null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (subLabel != null) {
                Text(
                    text       = subLabel,
                    color      = KEY_SUB,
                    fontSize   = 8.sp,
                    fontWeight = FontWeight.Normal,
                    modifier   = Modifier.offset(y = 2.dp)
                )
            }
            Text(
                text       = label,
                color      = textColor,
                fontSize   = fontSize.sp,
                fontWeight = fontWeight,
                lineHeight  = fontSize.sp
            )
        }
    }
}

// ── Full keyboard composable ──────────────────────────────────────────────────
@Composable
fun PocketKeyboard(
    isPasswordField: Boolean = false,
    onChar: (String) -> Unit,
    onBackspace: () -> Unit,
    onDone: () -> Unit
) {
    var mode      by remember { mutableStateOf(KeyboardMode.LOWER) }
    var capsLock  by remember { mutableStateOf(false) }   // double-tap shift = caps lock

    val keyH: Dp = 44.dp

    fun emit(char: String) {
        val out = if (mode == KeyboardMode.UPPER) char.uppercase() else char
        onChar(out)
        // Auto-revert to lower after one uppercase char (unless caps lock)
        if (mode == KeyboardMode.UPPER && !capsLock) mode = KeyboardMode.LOWER
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BG)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // ── Row: numbers ──────────────────────────────────────────────────────
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            val row = if (mode == KeyboardMode.SYMBOLS) ROW_SPECIALS else ROW_NUMBERS
            row.forEach { ch ->
                Key(
                    label    = ch,
                    modifier = Modifier.weight(1f).height(keyH),
                    bgColor  = KEY_ACCENT,
                    fontSize = 14,
                    onClick  = { emit(ch) }
                )
            }
        }

        // ── Row 1: QWERTY ─────────────────────────────────────────────────────
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(0.2f))
            ROW_1.forEach { ch ->
                val display = if (mode == KeyboardMode.UPPER) ch.uppercase() else ch
                Key(
                    label    = display,
                    modifier = Modifier.weight(1f).height(keyH),
                    onClick  = { emit(ch) }
                )
            }
            Spacer(Modifier.weight(0.2f))
        }

        // ── Row 2: ASDFGHJKL ──────────────────────────────────────────────────
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(0.5f))
            ROW_2.forEach { ch ->
                val display = if (mode == KeyboardMode.UPPER) ch.uppercase() else ch
                Key(
                    label    = display,
                    modifier = Modifier.weight(1f).height(keyH),
                    onClick  = { emit(ch) }
                )
            }
            Spacer(Modifier.weight(0.5f))
        }

        // ── Row 3: Shift + ZXCVBNM + Backspace ───────────────────────────────
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Shift key
            var lastShiftTap by remember { mutableStateOf(0L) }
            Key(
                label      = when (mode) {
                    KeyboardMode.UPPER -> if (capsLock) "⇪" else "⬆"
                    else               -> "⬆"
                },
                modifier   = Modifier.weight(1.4f).height(keyH),
                bgColor    = if (mode == KeyboardMode.UPPER) KEY_ACTION.copy(0.3f) else KEY_SPECIAL,
                textColor  = if (mode == KeyboardMode.UPPER) KEY_ACTION else KEY_TEXT,
                fontWeight = FontWeight.Bold,
                fontSize   = 14,
                onClick    = {
                    val now = System.currentTimeMillis()
                    if (now - lastShiftTap < 400) {
                        // Double-tap = caps lock
                        capsLock = !capsLock
                        mode = if (capsLock) KeyboardMode.UPPER else KeyboardMode.LOWER
                    } else {
                        capsLock = false
                        mode = if (mode == KeyboardMode.UPPER) KeyboardMode.LOWER else KeyboardMode.UPPER
                    }
                    lastShiftTap = now
                }
            )

            ROW_3.forEach { ch ->
                val display = if (mode == KeyboardMode.UPPER) ch.uppercase() else ch
                Key(
                    label    = display,
                    modifier = Modifier.weight(1f).height(keyH),
                    onClick  = { emit(ch) }
                )
            }

            // Backspace
            Key(
                label      = "⌫",
                modifier   = Modifier.weight(1.4f).height(keyH),
                bgColor    = KEY_SPECIAL,
                textColor  = KEY_TEXT,
                fontWeight = FontWeight.Bold,
                fontSize   = 16,
                onClick    = onBackspace
            )
        }

        // ── Row 4: extra special chars + space + done ─────────────────────────
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Symbols toggle
            Key(
                label     = if (mode == KeyboardMode.SYMBOLS) "ABC" else "!@#",
                modifier  = Modifier.weight(1.6f).height(keyH),
                bgColor   = KEY_SPECIAL,
                fontSize  = 11,
                fontWeight = FontWeight.Bold,
                onClick   = {
                    mode = if (mode == KeyboardMode.SYMBOLS) KeyboardMode.LOWER else KeyboardMode.SYMBOLS
                }
            )

            // Extra special chars row
            ROW_EXTRA.forEach { ch ->
                Key(
                    label    = ch,
                    modifier = Modifier.weight(0.9f).height(keyH),
                    bgColor  = KEY_ACCENT,
                    fontSize = 14,
                    onClick  = { onChar(ch) }
                )
            }

            // Done / Next
            Key(
                label      = if (isPasswordField) "GO" else "→",
                modifier   = Modifier.weight(1.6f).height(keyH),
                bgColor    = KEY_ACTION,
                textColor  = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize   = 13,
                onClick    = onDone
            )
        }

        Spacer(Modifier.height(4.dp))
    }
}