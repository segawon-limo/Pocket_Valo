package com.pocketvalo.app.data.remote.api

import com.pocketvalo.app.data.model.AccountResponse
import com.pocketvalo.app.data.model.MatchDetailResponse
import com.pocketvalo.app.data.model.MatchHistoryResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface HenrikApiService {

    @GET("valorant/v1/account/{name}/{tag}")
    suspend fun getAccount(
        @Path("name") name: String,
        @Path("tag") tag: String,
        @Header("Authorization") apiKey: String
    ): Response<AccountResponse>

    @GET("valorant/v3/matches/{region}/{name}/{tag}")
    suspend fun getMatchHistory(
        @Path("region") region: String,
        @Path("name") name: String,
        @Path("tag") tag: String,
        @Query("size") size: Int = 30,
        @Header("Authorization") apiKey: String
    ): Response<MatchHistoryResponse>

    @GET("valorant/v2/match/{matchId}")
    suspend fun getMatchDetail(
        @Path("matchId") matchId: String,
        @Header("Authorization") apiKey: String
    ): Response<MatchDetailResponse>
}