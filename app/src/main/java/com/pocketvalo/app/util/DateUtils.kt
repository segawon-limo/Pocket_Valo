package com.pocketvalo.app.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * Format epoch seconds (dari Henrik API game_start) ke local timezone device.
 * Output: "Saturday, May 23, 2026 11:02 PM" (atau sesuai locale)
 */
fun formatMatchDateTime(epochSeconds: Long): String {
    if (epochSeconds <= 0L) return ""
    val date = Date(epochSeconds * 1000L)
    val sdf = SimpleDateFormat("EEEE, MMM dd, yyyy hh:mm a", Locale.getDefault())
    sdf.timeZone = TimeZone.getDefault()   // local timezone device
    return sdf.format(date)
}

fun formatMatchDateOnly(epochSeconds: Long): String {
    if (epochSeconds <= 0L) return ""
    val date = Date(epochSeconds * 1000L)
    val sdf = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
    sdf.timeZone = TimeZone.getDefault()
    return sdf.format(date)
}

fun formatMatchTimeOnly(epochSeconds: Long): String {
    if (epochSeconds <= 0L) return ""
    val date = Date(epochSeconds * 1000L)
    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
    sdf.timeZone = TimeZone.getDefault()
    return sdf.format(date)
}