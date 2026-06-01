package com.pocketvalo.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.pocketvalo.app.MainActivity
import com.pocketvalo.app.R
import com.pocketvalo.app.data.local.AppDatabase
import com.pocketvalo.app.data.local.MultiAccountTokenStorage
import com.pocketvalo.app.data.local.TokenStorage
import com.pocketvalo.app.data.repository.AuthResult
import com.pocketvalo.app.data.repository.RiotAuthRepository
import com.pocketvalo.app.data.repository.StoreRepository
import java.util.concurrent.TimeUnit
import java.util.Calendar
import java.util.TimeZone

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
        ?: return Result.success() // Tidak ada akun, skip

        // Cek watchlist akun aktif dulu
        val watchlist = db.watchlistDao().getWatchlistForAccountOnce(activePuuid)
        if (watchlist.isEmpty()) return Result.success()

        // Ensure token valid
        val tokenResult = authRepo.ensureValidToken()
        if (tokenResult is AuthResult.Failure) return Result.retry()

        // Fetch store
        val storeRepo = StoreRepository(tokenStorage, authRepo, db.storeDao())
        val storeResult = storeRepo.getStore(forceRefresh = true)
        if (storeResult is AuthResult.Failure) return Result.retry()

        val store = (storeResult as AuthResult.Success).data

        // Match: store skin level UUIDs vs watchlist level UUIDs
        val storeLevelUuids = store.skinUuids.toSet()
        val matches = watchlist.filter { it.levelUuid in storeLevelUuids }

        if (matches.isNotEmpty()) {
            sendNotification(matches.map { it.displayName })
        }

        return Result.success()
    }

    private fun sendNotification(skinNames: List<String>) {
        val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel (required API 26+)
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
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (skinNames.size == 1)
            "🛒 ${skinNames[0]} ada di store!"
        else
            "🛒 ${skinNames.size} skin wishlist ada di store!"

        val body = if (skinNames.size == 1)
            "Skin favoritmu muncul di Daily Store hari ini."
        else
            skinNames.joinToString(", ")

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notifManager.notify(NOTIF_ID, notif)
    }

    companion object {
        const val CHANNEL_ID = "watchlist_alerts"
        const val NOTIF_ID   = 1001
        const val WORK_NAME  = "watchlist_daily_check"

        /**
         * Schedule daily check pada 00:30 UTC.
         * Menghitung delay dari sekarang ke 00:30 UTC berikutnya.
         */
        fun schedule(context: Context) {
            val now = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            val target = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 30)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                // Kalau 00:30 UTC hari ini sudah lewat, jadwalkan besok
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
                ExistingPeriodicWorkPolicy.KEEP, // jangan reschedule kalau sudah ada
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}