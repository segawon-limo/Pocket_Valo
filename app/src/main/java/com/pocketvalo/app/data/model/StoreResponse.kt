package com.pocketvalo.app.data.model

import com.google.gson.annotations.SerializedName

// ── Riot Token endpoint response ─────────────────────────────────────────────
data class RiotTokenResponse(
    @SerializedName("access_token") val accessToken: String?,
    @SerializedName("id_token") val idToken: String?,
    @SerializedName("refresh_token") val refreshToken: String?,
    @SerializedName("expires_in") val expiresIn: Int?,
    @SerializedName("token_type") val tokenType: String?,
    @SerializedName("error") val error: String?
)

// ── Riot userinfo endpoint ────────────────────────────────────────────────────
data class RiotUserInfoResponse(
    @SerializedName("sub") val puuid: String?,
    @SerializedName("acct") val account: RiotAccount?
)

data class RiotAccount(
    @SerializedName("game_name") val gameName: String?,
    @SerializedName("tag_line") val tagLine: String?
)

// ── Entitlement token endpoint ────────────────────────────────────────────────
data class EntitlementResponse(
    @SerializedName("entitlements_token") val entitlementsToken: String?
)

// ── Region / PAS endpoint ─────────────────────────────────────────────────────
data class PasTokenResponse(
    @SerializedName("affinities") val affinities: PasAffinities?
)

data class PasAffinities(
    @SerializedName("live") val live: String?
)

// ── Store storefront endpoint ─────────────────────────────────────────────────
data class StorefrontResponse(
    @SerializedName("SkinsPanelLayout") val skinsPanelLayout: SkinsPanelLayout?,
    @SerializedName("FeaturedBundle") val featuredBundle: FeaturedBundle?,
    @SerializedName("BonusStore") val bonusStore: BonusStore?,
    @SerializedName("AccessoryStore") val accessoryStore: AccessoryStore?,
    @SerializedName("httpStatus") val httpStatus: Int?,
    @SerializedName("errorCode") val errorCode: String?
)

data class SkinsPanelLayout(
    @SerializedName("SingleItemOffers") val singleItemOffers: List<String>,
    @SerializedName("SingleItemOffersRemainingDurationInSeconds") val remainingDurationInSeconds: Long,
    // Berisi harga VP per skin — key = levelUuid (= OfferID), value = harga VP
    @SerializedName("SingleItemStoreOffers") val singleItemStoreOffers: List<SingleItemStoreOffer>?
)

data class SingleItemStoreOffer(
    @SerializedName("OfferID") val offerId: String,
    @SerializedName("IsDirectPurchase") val isDirectPurchase: Boolean,
    @SerializedName("Cost") val cost: Map<String, Int>?   // langsung di top level
) {
    companion object {
        const val VP_CURRENCY_UUID = "85ad13f7-3d1b-5128-9eb2-7cd8ee0b5741"
    }
    val vpCost: Int get() = cost?.get(VP_CURRENCY_UUID) ?: 0
}

data class FeaturedBundle(
    @SerializedName("Bundles") val bundles: List<RawBundle>
)

data class RawBundle(
    @SerializedName("DataAssetID") val uuid: String,
    @SerializedName("DurationRemainingInSeconds") val durationRemainingInSeconds: Long,
    @SerializedName("Items") val items: List<BundleItem>? = null,
    @SerializedName("TotalBaseCost") val totalBaseCost: Map<String, Int>? = null,
    @SerializedName("TotalDiscountedCost") val totalDiscountedCost: Map<String, Int>? = null,
    @SerializedName("TotalDiscountPercent") val totalDiscountPercent: Float? = null
)

data class BundleItem(
    @SerializedName("Item") val item: BundleItemDetail?,
    @SerializedName("BasePrice") val basePrice: Int = 0,
    @SerializedName("DiscountedPrice") val discountedPrice: Int = 0,
    @SerializedName("DiscountPercent") val discountPercent: Float = 0f,
    @SerializedName("IsPromoItem") val isPromoItem: Boolean = false
)

data class BundleItemDetail(
    @SerializedName("ItemTypeID") val itemTypeId: String?,
    @SerializedName("ItemID") val itemId: String?,
    @SerializedName("Amount") val amount: Int = 1
)

data class BonusStore(
    @SerializedName("BonusStoreOffers") val offers: List<BonusOffer>?,
    @SerializedName("BonusStoreRemainingDurationInSeconds") val remainingDurationInSeconds: Long?
)

data class BonusOffer(
    @SerializedName("Offer") val offer: StoreOffer,
    @SerializedName("DiscountPercent") val discountPercent: Int,
    @SerializedName("DiscountCosts") val discountCosts: Map<String, Int>
)

data class AccessoryStore(
    @SerializedName("AccessoryStoreOffers") val offers: List<AccessoryOffer>?,
    @SerializedName("AccessoryStoreRemainingDurationInSeconds") val remainingDurationInSeconds: Long
)

data class AccessoryOffer(
    @SerializedName("Offer") val offer: StoreOffer,
    @SerializedName("ContractID") val contractId: String
)

data class StoreOffer(
    @SerializedName("OfferID") val offerId: String,
    @SerializedName("Cost") val cost: Map<String, Int>,
    @SerializedName("Rewards") val rewards: List<StoreReward>
)

data class StoreReward(
    @SerializedName("ItemTypeID") val itemTypeId: String,
    @SerializedName("ItemID") val itemId: String,
    @SerializedName("Quantity") val quantity: Int
)

// ── Wallet endpoint ───────────────────────────────────────────────────────────
data class WalletResponse(
    @SerializedName("Balances") val balances: Map<String, Int>?
) {
    // VP currency UUID
    val vp: Int get() = balances?.get("85ad13f7-3d1b-5128-9eb2-7cd8ee0b5741") ?: 0
    // Radianite currency UUID
    val rad: Int get() = balances?.get("e59aa87c-4cbf-517a-5983-6e81511be9b7") ?: 0
}
// ── Bundle detail dari valorant-api.com ───────────────────────────────────────
data class BundleDetailResponse(
    @SerializedName("data") val data: BundleDetail?
)

data class BundleDetail(
    @SerializedName("uuid")         val uuid: String,
    @SerializedName("displayName")  val displayName: String,
    @SerializedName("description")  val description: String?,
    @SerializedName("displayIcon")  val displayIcon: String?,
    @SerializedName("displayIcon2") val displayIcon2: String?,
    @SerializedName("verticalPromoImage") val verticalPromoImage: String?,
    @SerializedName("useAdditionalContext") val useAdditionalContext: Boolean?,
    @SerializedName("weapons")      val weapons: List<BundleWeapon>?,
    @SerializedName("cards")        val cards: List<BundleCard>?,
    @SerializedName("price")        val price: Int?
)

data class BundleWeapon(
    @SerializedName("uuid")            val uuid: String,
    @SerializedName("displayName")     val displayName: String,
    @SerializedName("displayIcon")     val displayIcon: String?,
    @SerializedName("fullRender")      val fullRender: String?,
    @SerializedName("price")           val price: Int?,
    @SerializedName("discountedPrice")  val discountedPrice: Int?,
    @SerializedName("weapon")          val weapon: BundleWeaponInfo?,
    @SerializedName("levels")          val levels: List<BundleWeaponLevel>? = null
)

data class BundleWeaponLevel(
    @SerializedName("uuid")           val uuid: String,
    @SerializedName("streamedVideo")  val streamedVideo: String?
)

data class BundleWeaponInfo(
    @SerializedName("displayName") val displayName: String?,
    @SerializedName("category")    val category: String?
)

data class BundleCard(
    @SerializedName("uuid")        val uuid: String,
    @SerializedName("displayName") val displayName: String,
    @SerializedName("displayIcon") val displayIcon: String?,
    @SerializedName("price")       val price: Int?,
    @SerializedName("discountedPrice") val discountedPrice: Int?
)