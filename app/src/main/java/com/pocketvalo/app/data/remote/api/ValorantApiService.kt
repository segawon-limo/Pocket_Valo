package com.pocketvalo.app.data.remote.api

import com.pocketvalo.app.data.model.CompetitiveTierResponse
import com.pocketvalo.app.data.model.MapResponse
import com.pocketvalo.app.data.model.AgentResponse
import com.pocketvalo.app.data.model.PlayerCardResponse
import com.pocketvalo.app.data.model.WeaponResponse
import retrofit2.Response
import retrofit2.http.GET

interface ValorantApiService {

    @GET("v1/competitivetiers")
    suspend fun getCompetitiveTiers(): Response<CompetitiveTierResponse>

    @GET("v1/maps")
    suspend fun getMaps(): Response<MapResponse>

    @GET("v1/agents")
    suspend fun getAgents(
        @retrofit2.http.Query("isPlayableCharacter") isPlayableCharacter: Boolean = true
    ): Response<AgentResponse>

    @GET("v1/weapons")
    suspend fun getWeapons(): Response<WeaponResponse>

    @GET("v1/playercards")
    suspend fun getPlayerCards(): Response<PlayerCardResponse>
}