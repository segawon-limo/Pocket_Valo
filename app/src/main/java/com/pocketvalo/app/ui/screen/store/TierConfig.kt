package com.pocketvalo.app.ui.screen.store

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.pocketvalo.app.R

data class TierConfig(
    val backgroundStart: Color,
    val backgroundEnd: Color,
    @DrawableRes val badgeRes: Int?
)

fun tierConfigFromUuid(uuid: String?): TierConfig = when (uuid) {
    "12683d76-48d7-84a3-4e09-6985794f0445" -> TierConfig(Color(0xFF2E567A), Color(0xFF003333), R.drawable.tier_select)
    "0cebb8be-46d7-c12a-d306-e9907bfc5a25" -> TierConfig(Color(0xFF025048), Color(0xFF051829), R.drawable.tier_deluxe)
    "60bca009-4182-7998-dee7-b8a2558dc369" -> TierConfig(Color(0xFF722A4C), Color(0xFF3A0515), R.drawable.tier_premium)
    "411e4a55-4e59-7757-41f0-86a53f101bb5" -> TierConfig(Color(0xFFC1AC5F), Color(0xFF2A1500), R.drawable.tier_ultra)
    "e046854e-406c-37f4-6607-19a9ba8426fc" -> TierConfig(Color(0xFF844F2C), Color(0xFF2A0E00), R.drawable.tier_exclusive)
    else -> TierConfig(Color(0xFF4A4A4A), Color(0xFF111111), null)
}

const val VP_ICON_URL = "https://media.valorant-api.com/currencies/85ad13f7-3d1b-5128-9eb2-7cd8ee0b5741/displayicon.png"