package com.pocketvalo.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.pocketvalo.app.MainActivity
import com.pocketvalo.app.R
import com.pocketvalo.app.data.local.AppDatabase
import com.pocketvalo.app.data.local.MultiAccountTokenStorage
import com.pocketvalo.app.data.local.TokenStorage
import com.pocketvalo.app.data.local.entity.WatchlistEntity
import com.pocketvalo.app.data.repository.AssetsRepository
import com.pocketvalo.app.data.repository.AuthResult
import com.pocketvalo.app.data.repository.RiotAuthRepository
import com.pocketvalo.app.data.repository.StoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class WatchlistWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val db           = AppDatabase.getInstance(context)
        val tokenStorage = TokenStorage(context)
        val multiStorage = MultiAccountTokenStorage(context)
        val authRepo     = RiotAuthRepository(tokenStorage, multiStorage)

        val activePuuid = multiStorage.activePuuid ?: tokenStorage.puuid
        ?: return Result.success()

        val watchlist = db.watchlistDao().getWatchlistForAccountOnce(activePuuid)
        if (watchlist.isEmpty()) return Result.success()

        val tokenResult = authRepo.ensureValidToken()
        if (tokenResult is AuthResult.Failure) return Result.retry()

        val storeRepo   = StoreRepository(tokenStorage, authRepo, db.storeDao())
        val storeResult = storeRepo.getStore(forceRefresh = true)
        if (storeResult is AuthResult.Failure) return Result.retry()

        val store           = (storeResult as AuthResult.Success).data
        val storeLevelUuids = store.skinUuids.toSet()
        val matches         = watchlist.filter { it.levelUuid in storeLevelUuids }

        if (matches.isNotEmpty()) {
            // Satu skin match — tampilkan dengan gambar
            // Banyak skin match — tampilkan satu per satu dengan notif ID berbeda
            matches.forEachIndexed { index, skin ->
                val bitmap = downloadBitmap(skin.iconUrl)
                sendNotification(
                    id       = NOTIF_ID + index,
                    skin     = skin,
                    bitmap   = bitmap,
                    total    = matches.size,
                    position = index + 1
                )
            }
        }

        return Result.success()
    }

    private suspend fun downloadBitmap(url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection().apply {
                    connectTimeout = 5000
                    readTimeout    = 5000
                }
                BitmapFactory.decodeStream(connection.getInputStream())
            } catch (e: Exception) {
                android.util.Log.w("WatchlistWorker", "Failed to download skin image: ${e.message}")
                null
            }
        }
    }

    private fun sendNotification(
        id: Int,
        skin: WatchlistEntity,
        bitmap: Bitmap?,
        total: Int,
        position: Int
    ) {
        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Watchlist Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi saat skin wishlist muncul di store"
            }
            notifManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "store")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (total == 1)
            "🛒 ${skin.displayName} ada di store!"
        else
            "🛒 ${skin.displayName} ($position/$total skin wishlist)"

        val body = "Skin favoritmu muncul di Daily Store hari ini. Jangan sampai kehabisan!"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (bitmap != null) {
            // BigPictureStyle — gambar skin ditampilkan besar di notifikasi
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap)
                    .bigLargeIcon(null as Bitmap?) // hide large icon saat expanded
                    .setBigContentTitle(title)
                    .setSummaryText(body)
            )
            // LargeIcon — thumbnail skin di sebelah teks (saat collapsed)
            builder.setLargeIcon(bitmap)
        } else {
            // Fallback tanpa gambar
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        notifManager.notify(id, builder.build())
    }

    companion object {
        const val CHANNEL_ID = "watchlist_alerts"
        const val NOTIF_ID   = 1001
        const val WORK_NAME  = "watchlist_daily_check"

        fun schedule(context: Context) {
            val now    = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            val target = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 30)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
            }

            val delayMs = target.timeInMillis - now.timeInMillis

            val request = PeriodicWorkRequestBuilder<WatchlistWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}