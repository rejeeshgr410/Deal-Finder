package com.example.ui.coupons

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.DealFinderViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UaeStoreCoupon(
    val id: String,
    val storeName: String,
    val domain: String,
    val discountLabel: String,
    val code: String,
    val category: String,
    val description: String,
    val accentColor: Color,
    val cardBgColor: Color,
    val isFeatured: Boolean = false
)

private val uaeTrendingCoupons = listOf(
    UaeStoreCoupon("noon", "Noon UAE", "noon.com", "10% OFF", "ALC44", "Shopping", "Cashback & sitewide", Color(0xFFCA8A04), Color(0xFFFEF9C3), isFeatured = true),
    UaeStoreCoupon("amazon", "Amazon.ae", "amazon.ae", "15% OFF", "CLIP15", "Shopping", "UAE Prime & bank deals", Color(0xFFD97706), Color(0xFFFFEDD5), isFeatured = true),
    UaeStoreCoupon("namshi", "Namshi UAE", "namshi.com", "20% OFF", "ACP57", "Fashion", "Apparel & footwear", Color(0xFF0F766E), Color(0xFFCCFBF1)),
    UaeStoreCoupon("shein", "SHEIN UAE", "shein.com", "15% OFF", "SHEIN15", "Fashion", "Extra off sitewide", Color(0xFF1E293B), Color(0xFFF1F5F9)),
    UaeStoreCoupon("temu", "Temu UAE", "temu.com", "30% OFF", "TEM13", "Shopping", "Sitewide & bundles", Color(0xFFEA580C), Color(0xFFFFEDD5)),
    UaeStoreCoupon("trendyol", "Trendyol UAE", "trendyol.com", "15% OFF", "WELCOME15", "Fashion", "Turkish fashion & home", Color(0xFFEA580C), Color(0xFFFFF7ED)),
    UaeStoreCoupon("ounass", "Ounass UAE", "ounass.ae", "10% OFF", "APP10", "Fashion", "Luxury designer edits", Color(0xFF334155), Color(0xFFF8FAFC)),
    UaeStoreCoupon("sivvi", "SIVVI UAE", "sivvi.com", "30% OFF", "SGG200", "Fashion", "Extra off sale & new", Color(0xFF7C3AED), Color(0xFFF3E8FF)),
    UaeStoreCoupon("6thstreet", "6thStreet UAE", "6thstreet.com", "16% OFF", "AR79", "Fashion", "Footwear & lifestyle", Color(0xFF0284C7), Color(0xFFE0F2FE)),
    UaeStoreCoupon("vogacloset", "VogaCloset UAE", "vogacloset.com", "40% OFF", "NEG", "Fashion", "UK & EU fashion", Color(0xFFBE185D), Color(0xFFFCE7F3)),
    UaeStoreCoupon("bloomingdales", "Bloomingdale's", "bloomingdales.ae", "20% OFF", "C25", "Fashion", "Luxury fashion & beauty", Color(0xFF1F2937), Color(0xFFF3F4F6)),
    UaeStoreCoupon("farfetch", "Farfetch UAE", "farfetch.com", "15% OFF", "FF15", "Fashion", "Designer collections", Color(0xFF0F172A), Color(0xFFE2E8F0)),
    UaeStoreCoupon("levelshoes", "Level Shoes", "levelshoes.com", "10% OFF", "AAD44", "Fashion", "Luxury shoes & bags", Color(0xFF475569), Color(0xFFF1F5F9)),
    UaeStoreCoupon("sephora", "Sephora UAE", "sephora.ae", "7% OFF", "ALC5", "Beauty", "Makeup & skincare", Color(0xFF111827), Color(0xFFF3F4F6)),
    UaeStoreCoupon("bbw", "Bath & Body Works", "bathandbodyworks.ae", "10% OFF", "PF74", "Beauty", "Candles & body care", Color(0xFF1D4ED8), Color(0xFFDBEAFE)),
    UaeStoreCoupon("faces", "Faces UAE", "faces.ae", "15% OFF", "CW78", "Beauty", "Fragrances & beauty", Color(0xFF9333EA), Color(0xFFF3E8FF)),
    UaeStoreCoupon("vs", "Victoria's Secret", "victoriassecret.ae", "15% OFF", "PFXA", "Beauty", "Mists & sleepwear", Color(0xFFDB2777), Color(0xFFFCE7F3)),
    UaeStoreCoupon("centrepoint", "Centrepoint", "centrepointstores.com", "15% OFF", "CPALC", "Fashion", "Fashion, baby & beauty", Color(0xFFDC2626), Color(0xFFFEE2E2)),
    UaeStoreCoupon("max", "Max Fashion UAE", "maxfashion.com", "10% OFF", "YD6", "Fashion", "Family value fashion", Color(0xFF2563EB), Color(0xFFDBEAFE)),
    UaeStoreCoupon("splash", "Splash UAE", "splashfashions.com", "20% OFF", "EPA", "Fashion", "Trendy apparel", Color(0xFFE11D48), Color(0xFFFFE4E6)),
    UaeStoreCoupon("homecentre", "Home Centre", "homecentre.com", "10% OFF", "HCMISSU10", "Home", "Furniture & decor", Color(0xFFB45309), Color(0xFFFEF3C7)),
    UaeStoreCoupon("homebox", "Home Box UAE", "homeboxstores.com", "15% OFF", "HO15", "Home", "Modern homeware", Color(0xFF0D9488), Color(0xFFCCFBF1)),
    UaeStoreCoupon("hm", "H&M UAE", "ae.hm.com", "10% OFF", "PF347", "Fashion", "Apparel & basics", Color(0xFFB91C1C), Color(0xFFFEE2E2)),
    UaeStoreCoupon("adidas", "Adidas UAE", "adidas.ae", "15% OFF", "RAN15", "Sports", "Sneakers & sportswear", Color(0xFF0F172A), Color(0xFFE2E8F0)),
    UaeStoreCoupon("puma", "Puma UAE", "ae.puma.com", "15% OFF", "P16", "Sports", "Running & training", Color(0xFFDC2626), Color(0xFFFEE2E2)),
    UaeStoreCoupon("sss", "Sun & Sand Sports", "sssports.com", "20% OFF", "ALC10", "Sports", "Sports gear & shoes", Color(0xFFEA580C), Color(0xFFFFEDD5)),
    UaeStoreCoupon("footlocker", "Foot Locker UAE", "footlocker.ae", "10% OFF", "A14M", "Sports", "Sneakers & streetwear", Color(0xFF1E293B), Color(0xFFF1F5F9)),
    UaeStoreCoupon("mumzworld", "Mumzworld UAE", "mumzworld.com", "17% OFF", "ACP", "Kids", "Mother, baby & toys", Color(0xFF059669), Color(0xFFD1FAE5)),
    UaeStoreCoupon("firstcry", "FirstCry UAE", "firstcry.ae", "10% OFF", "FA42", "Kids", "Kids apparel & gear", Color(0xFFD97706), Color(0xFFFEF3C7)),
    UaeStoreCoupon("iherb", "iHerb UAE", "iherb.com", "20% OFF", "LNI1840", "Health", "Vitamins & wellness", Color(0xFF16A34A), Color(0xFFDCFCE7)),
    UaeStoreCoupon("eyewa", "Eyewa UAE", "eyewa.com", "10% OFF", "VCME", "Beauty", "Sunglasses & lenses", Color(0xFF4F46E5), Color(0xFFE0E7FF)),
    UaeStoreCoupon("carrefour", "Carrefour UAE", "carrefouruae.com", "35% OFF", "HELLO40", "Food & Grocery", "First app grocery order", Color(0xFF1D4ED8), Color(0xFFDBEAFE)),
    UaeStoreCoupon("talabat", "Talabat UAE", "talabat.com", "AED 30 OFF", "ALC30", "Food & Grocery", "Food & Mart orders", Color(0xFFEA580C), Color(0xFFFFEDD5)),
    UaeStoreCoupon("deliveroo", "Deliveroo UAE", "deliveroo.ae", "50% OFF", "DCM11NEW", "Food & Grocery", "Restaurants & groceries", Color(0xFF0D9488), Color(0xFFCCFBF1)),
    UaeStoreCoupon("aliexpress", "AliExpress UAE", "aliexpress.com", "50% OFF", "khasemNE9", "Shopping", "Tech, gadgets & home", Color(0xFFDC2626), Color(0xFFFEE2E2))
).take(30)

private val couponCategories = listOf(
    "All", "Shopping", "Fashion", "Beauty", "Home", "Sports", "Kids", "Health", "Food & Grocery"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponsScreen(viewModel: DealFinderViewModel) {
    val firestoreCoupons by viewModel.coupons.collectAsState()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var copiedCode by remember { mutableStateOf<String?>(null) }

    val (merchantCoupons, mergedUaeCoupons) = remember(firestoreCoupons) {
        val global = firestoreCoupons.filter { it.isGlobal || it.domain.isNotBlank() }
        val shopOnly = firestoreCoupons.filter { !it.isGlobal && it.domain.isBlank() }

        val palette = listOf(
            Color(0xFF0F766E) to Color(0xFFCCFBF1),
            Color(0xFF7C3AED) to Color(0xFFF3E8FF),
            Color(0xFFD97706) to Color(0xFFFFEDD5),
            Color(0xFF0284C7) to Color(0xFFE0F2FE),
            Color(0xFFDB2777) to Color(0xFFFCE7F3)
        )
        val remoteMapped = global.mapIndexed { idx, c ->
            val name = c.storeName.ifBlank { c.title }
            val existing = uaeTrendingCoupons.find {
                (c.domain.isNotBlank() && it.domain.equals(c.domain, ignoreCase = true)) ||
                    it.storeName.equals(name, ignoreCase = true)
            }
            val (accent, bg) = if (existing != null) {
                existing.accentColor to existing.cardBgColor
            } else {
                palette[idx % palette.size]
            }
            UaeStoreCoupon(
                id = c.id.ifBlank { existing?.id ?: "remote_$idx" },
                storeName = name,
                domain = c.domain.ifBlank { existing?.domain ?: "" },
                discountLabel = c.discountInfo.ifBlank { existing?.discountLabel ?: "DEAL" },
                code = c.code,
                category = c.category.ifBlank { existing?.category ?: "Shopping" },
                description = c.description.ifBlank { existing?.description ?: c.title },
                accentColor = accent,
                cardBgColor = bg,
                isFeatured = c.isFeatured || (existing?.isFeatured == true)
            )
        }
        val overriddenKeys = remoteMapped.map { it.domain.lowercase() to it.storeName.lowercase() }.toSet()
        val remainingBuiltIn = uaeTrendingCoupons.filter { builtIn ->
            (builtIn.domain.lowercase() to builtIn.storeName.lowercase()) !in overriddenKeys
        }
        shopOnly to (remoteMapped + remainingBuiltIn)
    }

    val filteredStores = remember(mergedUaeCoupons, searchQuery, selectedCategory) {
        mergedUaeCoupons.filter { item ->
            val matchesCategory = selectedCategory == "All" || item.category == selectedCategory
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                item.storeName.lowercase().contains(q) ||
                item.code.lowercase().contains(q) ||
                item.category.lowercase().contains(q) ||
                item.description.lowercase().contains(q)
            matchesCategory && matchesQuery
        }
    }

    fun copyCouponCode(storeName: String, code: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Coupon Code", code))
        copiedCode = code
        Toast.makeText(context, "$storeName code copied: $code", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("UAE Coupons", fontWeight = FontWeight.Bold)
                        Text(
                            "Top ${uaeTrendingCoupons.size} UAE stores • Tap to copy",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(2) }) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search UAE stores or codes (e.g. Noon, Shein)...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item(span = { GridItemSpan(2) }) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(couponCategories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }
            }

            if (merchantCoupons.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Text(
                        "Merchant & Shop Coupons",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(merchantCoupons, span = { GridItemSpan(2) }) { coupon ->
                    val isCopied = copiedCode == coupon.code
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    coupon.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                if (coupon.expiry > 0) {
                                    val formattedDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                        .format(Date(coupon.expiry))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFFFE4E6)
                                    ) {
                                        Text(
                                            "Expires: $formattedDate",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFE11D48),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                            if (coupon.discountInfo.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    coupon.discountInfo,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            CodeCopyPill(
                                code = coupon.code.ifBlank { "NO CODE" },
                                enabled = coupon.code.isNotBlank(),
                                isCopied = isCopied,
                                accentColor = MaterialTheme.colorScheme.primary,
                                onCopy = { copyCouponCode(coupon.title, coupon.code) }
                            )
                        }
                    }
                }
            }

            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Trending UAE Stores",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${filteredStores.size} stores",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (filteredStores.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.ConfirmationNumber,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No matching UAE store coupons", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                items(
                    items = filteredStores,
                    key = { it.id },
                    span = { store -> GridItemSpan(if (store.isFeatured && searchQuery.isBlank() && selectedCategory == "All") 2 else 1) }
                ) { store ->
                    BentoCouponCard(
                        store = store,
                        isCopied = copiedCode == store.code,
                        onCopy = { copyCouponCode(store.storeName, store.code) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BentoCouponCard(
    store: UaeStoreCoupon,
    isCopied: Boolean,
    onCopy: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = store.cardBgColor,
        border = BorderStroke(1.dp, store.accentColor.copy(alpha = 0.22f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        var showFallback by remember(store.domain) { mutableStateOf(true) }
                        if (showFallback) {
                            Text(
                                text = store.storeName.take(1),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = store.accentColor
                            )
                        }
                        AsyncImage(
                            model = "https://www.google.com/s2/favicons?domain=${store.domain}&sz=128",
                            contentDescription = "${store.storeName} logo",
                            contentScale = ContentScale.Fit,
                            onSuccess = { showFallback = false },
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = store.accentColor
                ) {
                    Text(
                        text = store.discountLabel,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }

            Column {
                Text(
                    text = store.storeName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = store.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF475569),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            CodeCopyPill(
                code = store.code,
                enabled = true,
                isCopied = isCopied,
                accentColor = store.accentColor,
                onCopy = onCopy
            )
        }
    }
}

@Composable
private fun CodeCopyPill(
    code: String,
    enabled: Boolean,
    isCopied: Boolean,
    accentColor: Color,
    onCopy: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onCopy)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = code,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (enabled) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCopied) Color(0xFF16A34A) else accentColor.copy(alpha = 0.14f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = "Copy code",
                            tint = if (isCopied) Color.White else accentColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isCopied) "Copied" else "Copy",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCopied) Color.White else accentColor
                        )
                    }
                }
            }
        }
    }
}
