package com.pocketvalo.app.data.remote.api

import com.pocketvalo.app.data.model.AgentResponse
import com.pocketvalo.app.data.model.CompetitiveTierResponse
import com.pocketvalo.app.data.model.LevelBorderResponse
import com.pocketvalo.app.data.model.MapResponse
import com.pocketvalo.app.data.model.PlayerCardResponse
import com.pocketvalo.app.data.model.PlayerTitleResponse
import com.pocketvalo.app.data.model.WeaponResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

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

    @GET("v1/playertitles/{uuid}")
    suspend fun getPlayerTitle(
        @Path("uuid") uuid: String
    ): Response<PlayerTitleResponse>

    @GET("v1/levelborders")
    suspend fun getLevelBorders(): Response<LevelBorderResponse>

    @GET("v1/bundles/{uuid}")
    suspend fun getBundle(
        @Path("uuid") uuid: String
    ): Response<com.pocketvalo.app.data.model.BundleDetailResponse>

    @GET("v1/bundles/{uuid}")
    suspend fun getBundleRaw(
        @Path("uuid") uuid: String
    ): Response<okhttp3.ResponseBody>

    @GET("v1/version")
    suspend fun getVersion(): Response<com.pocketvalo.app.data.model.VersionResponse>
}
// Extension — tambah setelah interface original (file di-append)