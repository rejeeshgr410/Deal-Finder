package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DemoWalletPreset(
    val id: String,
    val name: String,
    val subtitle: String,
    val defaultPoints: String,
    val defaultCardNumber: String,
    val logoResUri: String,
    val stackedLogoResUri: String? = null,
    val cardBgColorHex: Long,
    val secondaryBgColorHex: Long,
    val accentColorHex: Long,
    val badgeTitle: String = "",
    val badgeSubtitle: String = ""
)

data class DemoCouponStyle(
    val code: String,
    val logoUri: String,
    val secondaryLabel: String = "",
    val bgTopHex: Long,
    val bgBottomHex: Long,
    val styleKey: String = ""
)

data class DemoCatalogSnapshot(
    val retailers: List<RetailerEntity> = emptyList(),
    val branches: List<BranchEntity> = emptyList(),
    val flyers: List<FlyerEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val promotions: List<PromotionEntity> = emptyList(),
    val coupons: List<CouponEntity> = emptyList(),
    val couponStylesByCode: Map<String, DemoCouponStyle> = emptyMap(),
    val walletSeedCards: List<LoyaltyCardEntity> = emptyList(),
    val walletPresets: List<DemoWalletPreset> = emptyList(),
    val notifications: List<NotificationEntity> = emptyList(),
    val trendingSearches: List<String> = emptyList(),
    val sampleShoppingItems: List<ShoppingListItem> = emptyList(),
    val clearanceBundles: List<ClearanceBundle> = emptyList()
)

interface DemoDataProvider {
    fun buildSnapshot(context: Context): DemoCatalogSnapshot
}

/**
 * Runtime registry connecting optional `src/demo/` temporary assets/catalog
 * cleanly to the app without any compile-time dependency on `com.example.demo.*`.
 * Deleting `app/src/demo/` or disabling demo mode leaves 100% live Firestore/Room behavior intact.
 */
object DemoRegistry {
    private const val PREFS_NAME = "demo_mode_prefs"
    private const val KEY_DEMO_ENABLED = "demo_enabled"
    private const val PROVIDER_CLASS = "com.example.demo.DemoDataBundle"

    private var appContext: Context? = null
    private var prefs: SharedPreferences? = null
    private var provider: DemoDataProvider? = null
    private var cachedSnapshot: DemoCatalogSnapshot? = null

    private val _isDemoAvailable = MutableStateFlow(false)
    val isDemoAvailable: StateFlow<Boolean> = _isDemoAvailable.asStateFlow()

    private val _isDemoEnabled = MutableStateFlow(false)
    val isDemoEnabled: StateFlow<Boolean> = _isDemoEnabled.asStateFlow()

    fun init(context: Context) {
        val ctx = context.applicationContext
        appContext = ctx
        val p = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = p

        val loadedProvider = try {
            val clazz = Class.forName(PROVIDER_CLASS)
            clazz.getDeclaredConstructor().newInstance() as? DemoDataProvider
        } catch (_: Throwable) {
            null
        }
        provider = loadedProvider
        val available = loadedProvider != null
        _isDemoAvailable.value = available
        _isDemoEnabled.value = if (available) p.getBoolean(KEY_DEMO_ENABLED, true) else false
        cachedSnapshot = if (loadedProvider != null && _isDemoEnabled.value) {
            loadedProvider.buildSnapshot(ctx)
        } else {
            null
        }
    }

    fun setDemoEnabled(enabled: Boolean) {
        val allowed = enabled && _isDemoAvailable.value
        prefs?.edit()?.putBoolean(KEY_DEMO_ENABLED, allowed)?.apply()
        _isDemoEnabled.value = allowed
        cachedSnapshot = if (allowed) {
            val ctx = appContext
            if (ctx != null) provider?.buildSnapshot(ctx) else null
        } else {
            null
        }
    }

    fun currentSnapshot(): DemoCatalogSnapshot {
        if (!_isDemoAvailable.value || !_isDemoEnabled.value) {
            return DemoCatalogSnapshot()
        }
        val existing = cachedSnapshot
        if (existing != null) return existing
        val ctx = appContext ?: return DemoCatalogSnapshot()
        return provider?.buildSnapshot(ctx)?.also { cachedSnapshot = it } ?: DemoCatalogSnapshot()
    }
}
