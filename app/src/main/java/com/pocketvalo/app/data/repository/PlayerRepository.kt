package com.pocketvalo.app.data.repository

import com.pocketvalo.app.data.model.AccountResponse
import com.pocketvalo.app.data.model.MatchHistoryResponse
import com.pocketvalo.app.data.remote.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

class PlayerRepository {

    private val api = RetrofitClient.henrikApi

    suspend fun getAccount(
        name: String,
        tag: String,
        apiKey: String
    ): Result<AccountResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAccount(name, tag, apiKey)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it)
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

    suspend fun getMatchHistory(
        region: String,
        name: String,
        tag: String,
        apiKey: String
    ): Result<MatchHistoryResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getMatchHistory(region, name, tag, apiKey)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.Success(it)
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
}