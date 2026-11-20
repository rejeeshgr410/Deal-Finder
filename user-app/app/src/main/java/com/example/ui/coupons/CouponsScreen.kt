package com.example.ui.coupons

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
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

    val (merchantCoupons, storeCoupons) = remember(firestoreCoupons) {
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
            val (accent, bg) = palette[idx % palette.size]
            UaeStoreCoupon(
                id = c.id.ifBlank { "remote_$idx" },
                storeName = name,
                domain = c.domain,
                discountLabel = c.discountInfo.ifBlank { "DEAL" },
                code = c.code,
                category = c.category.ifBlank { "Shopping" },
                description = c.description.ifBlank { c.title },
                accentColor = accent,
                cardBgColor = bg,
                isFeatured = c.isFeatured
            )
        }
        shopOnly to remoteMapped
    }

    val filteredMerchantCoupons = remember(merchantCoupons, searchQuery, selectedCategory) {
        val q = searchQuery.trim().lowercase()
        merchantCoupons.filter { coupon ->
            val matchesCategory = selectedCategory == "All" ||
                coupon.category.equals(selectedCategory, ignoreCase = true)
            val matchesQuery = q.isEmpty() ||
                coupon.title.lowercase().contains(q) ||
                coupon.code.lowercase().contains(q) ||
                coupon.discountInfo.lowercase().contains(q) ||
                coupon.description.lowercase().contains(q)
            matchesCategory && matchesQuery
        }
    }

    val filteredStores = remember(storeCoupons, searchQuery, selectedCategory) {
        val q = searchQuery.trim().lowercase()
        storeCoupons.filter { item ->
            val matchesCategory = selectedCategory == "All" ||
                item.category.equals(selectedCategory, ignoreCase = true)
            val matchesQuery = q.isEmpty() ||
                item.storeName.lowercase().contains(q) ||
                item.code.lowercase().contains(q) ||
                item.category.lowercase().contains(q) ||
                item.description.lowercase().contains(q)
            matchesCategory && matchesQuery
        }
    }

    fun copyCouponCode(storeName: String, code: String) {
        if (code.isBlank()) return
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
                            if (firestoreCoupons.isEmpty()) "Verified merchant & store codes"
                            else "${filteredMerchantCoupons.size + filteredStores.size} active offers • Tap to copy",
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
                    placeholder = { Text("Search stores, brands, or promo codes...") },
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

            if (filteredMerchantCoupons.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Text(
                        "Merchant & Shop Coupons",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(filteredMerchantCoupons, key = { it.id }, span = { GridItemSpan(2) }) { coupon ->
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

            if (filteredStores.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Verified Store Codes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${filteredStores.size} offers",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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

            if (filteredMerchantCoupons.isEmpty() && filteredStores.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.ConfirmationNumber,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (firestoreCoupons.isEmpty()) {
                                "No verified coupons available yet"
                            } else {
                                "No coupons match your search"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (firestoreCoupons.isEmpty()) {
                                "Verified merchant coupons will appear here once published."
                            } else {
                                "Try clearing your search or selecting All."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
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
                        if (store.domain.isNotBlank()) {
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
                code = store.code.ifBlank { "NO CODE" },
                enabled = store.code.isNotBlank(),
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
