package com.pocketvalo.app.data.remote.api

import com.pocketvalo.app.data.model.CompetitiveTierResponse
import com.pocketvalo.app.data.model.MapResponse
import retrofit2.Response
import retrofit2.http.GET

interface ValorantApiService {

    @GET("v1/competitivetiers")
    suspend fun getCompetitiveTiers(): Response<CompetitiveTierResponse>

    @GET("v1/maps")
    suspend fun getMaps(): Response<MapResponse>
}