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
import com.pocketvalo.app.data.local.AppDatabase
import com.pocketvalo.app.data.local.MultiAccountTokenStorage
import com.pocketvalo.app.data.local.TokenStorage
import com.pocketvalo.app.data.local.entity.AccountEntity
import com.pocketvalo.app.ui.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun SplashScreen(navController: NavController) {
    val context      = LocalContext.current
    val tokenStorage = remember { TokenStorage(context) }
    val multiStorage = remember { MultiAccountTokenStorage(context) }
    val db           = remember { AppDatabase.getInstance(context) }

    var cardImageUrl by remember { mutableStateOf<String?>(null) }
    var imageAlpha   by remember { mutableStateOf(0f) }
    var imageReady   by remember { mutableStateOf(false) }

    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by shimmerTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 1000f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )

    val imageAlphaAnim by animateFloatAsState(
        targetValue   = imageAlpha,
        animationSpec = tween(durationMillis = 700),
        label         = "image_fade"
    )

    // Routing logic:
    // - Belum login         → Welcome (untuk login)
    // - 1 akun tersimpan   → Loading langsung (no friction)
    // - 2+ akun tersimpan  → Welcome (untuk pilih akun)
    // - Token ada tapi DB kosong (MIUI backup stale) → clear token → Welcome
    var destination by remember { mutableStateOf(Screen.Welcome.route) }

    // Navigate after image loads
    LaunchedEffect(imageReady) {
        if (!imageReady) return@LaunchedEffect
        delay(1500L)
        navController.navigate(destination) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    LaunchedEffect(Unit) {
        // ── Deteksi stale token dari MIUI backup ──────────────────────────────
        // Jika token ada tapi Room DB tidak punya account → fresh install dengan
        // sisa data dari MIUI backup → clear semua token supaya user login ulang
        if (tokenStorage.isLoggedIn) {
            val accountCount = withContext(Dispatchers.IO) { db.accountDao().getAccountCount() }
            if (accountCount == 0) {
                // DB kosong tapi token ada → stale state → clear semua
                tokenStorage.clearAll()
                multiStorage.clearAll()
                destination = Screen.Welcome.route
                // Skip sisa logic, langsung ke navigate
            }
        }

        // ── Sync multiStorage ke Room DB ─────────────────────────────────────
        // MIUI backup bisa persist multiStorage dengan akun yang tidak ada di DB.
        // Hapus semua puuid dari multiStorage yang tidak punya entry di Room DB.
        val dbPuuids = withContext(Dispatchers.IO) {
            db.accountDao().getAllPuuids()
        }.toSet()

        multiStorage.getKnownPuuids().forEach { puuid ->
            if (puuid !in dbPuuids) {
                multiStorage.removeAccount(puuid)
                android.util.Log.d("SplashScreen", "Removed stale account from multiStorage: $puuid")
            }
        }

        // Kalau active puuid tidak ada di DB, clear active session
        val activePuuid = multiStorage.activePuuid
        if (activePuuid != null && activePuuid !in dbPuuids) {
            tokenStorage.clearAll()
            multiStorage.clearAll()
        }

        // Set destination berdasarkan state yang sudah bersih
        val knownAccounts = multiStorage.getKnownPuuids()
        destination = when {
            !tokenStorage.isLoggedIn -> Screen.Welcome.route
            knownAccounts.size > 1   -> Screen.Welcome.route
            else                     -> Screen.Loading.route
        }

        // Migrasi: pastikan akun aktif ada di Room DB (untuk user yang login sebelum fitur multi-account)
        val puuid    = tokenStorage.puuid
        val username = tokenStorage.username
        if (puuid != null && username != null) {
            val parts    = username.split("#")
            val gameName = parts.getOrNull(0) ?: username
            val tagLine  = parts.getOrNull(1) ?: ""
            withContext(Dispatchers.IO) {
                val existing = db.accountDao().getAccount("$gameName#$tagLine")
                if (existing == null) {
                    db.accountDao().upsertAccount(
                        AccountEntity(
                            riotId       = "$gameName#$tagLine",
                            gameName     = gameName,
                            tagLine      = tagLine,
                            puuid        = puuid,
                            region       = tokenStorage.region ?: "ap",
                            accountLevel = 0,
                            cardSmall    = null,
                            lastSearched = System.currentTimeMillis()
                        )
                    )
                }
            }
            // Juga pastikan MultiStorage kenal akun ini
            if (multiStorage.getKnownPuuids().isEmpty()) {
                val at = tokenStorage.accessToken ?: ""
                val rt = tokenStorage.refreshToken ?: ""
                if (at.isNotEmpty() && rt.isNotEmpty()) {
                    multiStorage.saveAccount(
                        puuid            = puuid,
                        accessToken      = at,
                        idToken          = tokenStorage.idToken ?: "",
                        refreshToken     = rt,
                        entitlementToken = tokenStorage.entitlementToken ?: "",
                        region           = tokenStorage.region ?: "ap",
                        username         = username,
                        expiresInSeconds = ((tokenStorage.accessTokenExpiresAt - System.currentTimeMillis()) / 1000)
                            .toInt().coerceAtLeast(0)
                    )
                    multiStorage.activePuuid = puuid
                }
            }
        }

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

        // Trigger imageReady fallback kalau image tidak load
        if (!imageReady) {
            imageReady = true
        }
        // Fallback: navigate even if image fails or is too slow
        delay(4000L)
        if (navController.currentBackStackEntry?.destination?.route == Screen.Splash.route) {
            navController.navigate(destination) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1923))
    ) {
        // Animated shimmer background
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
                        end   = Offset(shimmerOffset + 500f, 1000f)
                    )
                )
        )

        if (cardImageUrl != null) {
            AsyncImage(
                model            = cardImageUrl,
                contentDescription = null,
                modifier         = Modifier.fillMaxSize().alpha(imageAlphaAnim),
                contentScale     = ContentScale.Crop,
                onSuccess        = { imageAlpha = 1f; imageReady = true }
            )
        }

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f  to Color(0xFF0F1923).copy(alpha = 0.2f),
                            0.45f to Color(0xFF0F1923).copy(alpha = 0.4f),
                            1.0f  to Color(0xFF0F1923).copy(alpha = 0.95f)
                        )
                    )
                )
        )

        Column(
            modifier            = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text          = "POCKET",
                color         = Color(0xFFFF4655),
                fontSize      = 36.sp,
                fontWeight    = FontWeight.ExtraBold,
                letterSpacing = 8.sp
            )
            Text(
                text          = "VALO",
                color         = Color.White,
                fontSize      = 36.sp,
                fontWeight    = FontWeight.ExtraBold,
                letterSpacing = 8.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text          = "Your Valorant Companion",
                color         = Color(0xFF9BA3AF),
                fontSize      = 13.sp,
                letterSpacing = 1.sp,
                textAlign     = TextAlign.Center
            )
        }
    }
}