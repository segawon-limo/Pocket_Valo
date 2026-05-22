package com.pocketvalo.app.data.repository

import android.content.Context
import com.pocketvalo.app.data.local.AppDatabase
import com.pocketvalo.app.data.local.entity.AccountEntity
import com.pocketvalo.app.data.local.entity.MatchEntity
import com.pocketvalo.app.data.model.AccountResponse
import com.pocketvalo.app.data.model.MatchData
import com.pocketvalo.app.data.model.MatchDetailResponse
import com.pocketvalo.app.data.model.MatchHistoryResponse
import com.pocketvalo.app.data.remote.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

class PlayerRepository(context: Context) {

    private val api = RetrofitClient.henrikApi
    private val db = AppDatabase.getInstance(context)
    private val accountDao = db.accountDao()
    private val matchDao = db.matchDao()

    // ─── Accounts ────────────────────────────────────────────────────────────

    fun getSavedAccounts(): Flow<List<AccountEntity>> = accountDao.getAllAccounts()

    suspend fun getAccount(
        name: String,
        tag: String,
        apiKey: String
    ): Result<AccountResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAccount(name, tag, apiKey)
            if (response.isSuccessful) {
                response.body()?.let { accountResponse ->
                    accountResponse.data?.let { data ->
                        accountDao.upsertAccount(
                            AccountEntity(
                                riotId = "${data.name}#${data.tag}",
                                gameName = data.name,
                                tagLine = data.tag,
                                puuid = data.puuid,
                                region = data.region,
                                accountLevel = data.accountLevel,
                                cardSmall = data.card?.small,
                                lastSearched = System.currentTimeMillis()
                            )
                        )
                    }
                    Result.Success(accountResponse)
                } ?: Result.Error("Empty response")
            } else {
                when (response.code()) {
                    404 -> Result.Error("Player not found")
                    429 -> Result.Error("Rate limit exceeded. Try again later")
                    else -> Result.Error("Error ${response.code()}")
                }
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun deleteAccount(riotId: String) {
        accountDao.deleteAccount(riotId)
        matchDao.clearMatchHistory(riotId)
    }

    // ─── Match History ────────────────────────────────────────────────────────

    fun getCachedMatchHistory(riotId: String): Flow<List<MatchEntity>> =
        matchDao.getMatchHistory(riotId)

    suspend fun refreshMatchHistory(
        region: String,
        name: String,
        tag: String,
        apiKey: String
    ): Result<MatchHistoryResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getMatchHistory(region, name, tag, size = 30, apiKey)
            if (response.isSuccessful) {
                response.body()?.let { matchResponse ->
                    val riotId = "$name#$tag"
                    val entities = matchResponse.data?.mapIndexedNotNull { index, match ->
                        match.toEntity(riotId, name, tag, index)
                    } ?: emptyList()

                    if (entities.isNotEmpty()) {
                        matchDao.upsertMatches(entities)
                        matchDao.trimHistory(riotId, keep = 50)
                    }
                    Result.Success(matchResponse)
                } ?: Result.Error("Empty response")
            } else {
                when (response.code()) {
                    404 -> Result.Error("Player not found")
                    429 -> Result.Error("Rate limit exceeded. Try again later")
                    else -> Result.Error("Error ${response.code()}")
                }
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun getMatchDetail(matchId: String, apiKey: String): Result<MatchDetailResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getMatchDetail(matchId, apiKey)
                if (response.isSuccessful) {
                    response.body()?.let {
                        Result.Success(it)
                    } ?: Result.Error("Empty response")
                } else {
                    when (response.code()) {
                        404 -> Result.Error("Match not found")
                        429 -> Result.Error("Rate limit exceeded. Try again later")
                        else -> Result.Error("Error ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                Result.Error(e.message ?: "Network error")
            }
        }

    // ─── Mapper ──────────────────────────────────────────────────────────────

    private fun MatchData.toEntity(riotId: String, name: String, tag: String, index: Int): MatchEntity? {
        val player = players?.allPlayers?.find {
            it.name.equals(name, ignoreCase = true) && it.tag.equals(tag, ignoreCase = true)
        } ?: return null

        val playerTeam = player.team
        val teamData = if (playerTeam.equals("Red", ignoreCase = true)) teams?.red else teams?.blue

        return MatchEntity(
            matchId = metadata.matchId,
            ownerRiotId = riotId,
            map = metadata.map,
            mode = metadata.mode,
            gameStartPatched = metadata.gameStartPatched,
            gameLength = metadata.gameLength,
            roundsPlayed = metadata.roundsPlayed,
            agentName = player.character,
            agentPortraitUrl = player.assets?.agent?.small,
            kills = player.stats?.kills ?: 0,
            deaths = player.stats?.deaths ?: 0,
            assists = player.stats?.assists ?: 0,
            score = player.stats?.score ?: 0,
            rankName = player.rankName,
            playerTeam = playerTeam,
            redRoundsWon = teams?.red?.roundsWon ?: 0,
            blueRoundsWon = teams?.blue?.roundsWon ?: 0,
            hasWon = teamData?.hasWon ?: false,
            cachedAt = System.currentTimeMillis(),
            matchIndex = index
        )
    }
}