package com.pocketvalo.app.data.repository

import com.pocketvalo.app.data.model.TierData
import com.pocketvalo.app.data.model.MapData
import com.pocketvalo.app.data.model.AgentData
import com.pocketvalo.app.data.model.WeaponData
import com.pocketvalo.app.data.remote.api.RetrofitClient

class AssetsRepository {

    private val api = RetrofitClient.valorantApi
    private var cachedTiers: Map<String, TierData> = emptyMap()
    private var cachedMaps: Map<String, MapData> = emptyMap()
    private var cachedAgents: List<AgentData> = emptyList()
    private var cachedWeapons: List<WeaponData> = emptyList()

    suspend fun getCompetitiveTiers(): Result<Map<String, TierData>> {
        if (cachedTiers.isNotEmpty()) return Result.Success(cachedTiers)

        return try {
            val response = api.getCompetitiveTiers()
            if (response.isSuccessful) {
                val latestEpisode = response.body()?.data?.lastOrNull()
                val tierMap = latestEpisode?.tiers
                    ?.filter { it.smallIcon != null }
                    ?.associateBy { it.tierName.uppercase() }
                    ?: emptyMap()
                cachedTiers = tierMap
                Result.Success(tierMap)
            } else {
                Result.Error("Failed to load rank data")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun getMaps(): Result<Map<String, MapData>> {
        if (cachedMaps.isNotEmpty()) return Result.Success(cachedMaps)

        return try {
            val response = api.getMaps()
            if (response.isSuccessful) {
                val mapData = response.body()?.data
                    ?.associateBy { it.displayName.uppercase() }
                    ?: emptyMap()
                cachedMaps = mapData
                Result.Success(cachedMaps)
            } else {
                Result.Error("Failed to load map data")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun getAgents(): Result<List<AgentData>> {
        if (cachedAgents.isNotEmpty()) return Result.Success(cachedAgents)

        return try {
            val response = api.getAgents(isPlayableCharacter = true)
            if (response.isSuccessful) {
                val agents = response.body()?.data
                    ?.filter { it.isPlayableCharacter }
                    ?.sortedBy { it.displayName }
                    ?: emptyList()
                cachedAgents = agents
                Result.Success(agents)
            } else {
                Result.Error("Failed to load agents")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun getWeapons(): Result<List<WeaponData>> {
        if (cachedWeapons.isNotEmpty()) return Result.Success(cachedWeapons)

        val categoryOrder = mapOf(
            "Melee" to 0,
            "Sidearm" to 1,
            "SMG" to 2,
            "Shotgun" to 3,
            "Rifle" to 4,
            "Sniper" to 5,
            "Heavy" to 6
        )

        return try {
            val response = api.getWeapons()
            if (response.isSuccessful) {
                val weapons = response.body()?.data
                    ?.sortedWith(
                        compareBy(
                            { categoryOrder[it.category.substringAfterLast("::")] ?: 99 },
                            { it.shopData?.cost ?: 0 },
                            { it.displayName }
                        )
                    )
                    ?: emptyList()
                cachedWeapons = weapons
                Result.Success(weapons)
            } else {
                Result.Error("Failed to load weapons")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

}