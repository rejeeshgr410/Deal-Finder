package com.example.ui.loyalty

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.DemoRegistry
import com.example.data.DemoWalletPreset
import com.example.data.LocationManager
import com.example.data.LoyaltyCardDao
import com.example.data.LoyaltyCardEntity
import com.example.ui.theme.*
import kotlinx.coroutines.launch

data class PresetRetailer(
    val name: String,
    val programName: String,
    val colorHex: Long,
    val defaultFormat: String = "CODE_128"
)

val UAE_PRESET_RETAILERS = listOf(
    PresetRetailer("LuLu Happiness", "Happiness Loyalty", 0xFF5E1B9E),
    PresetRetailer("Nesto INAAM", "INAAM Rewards", 0xFF121F30),
    PresetRetailer("Carrefour SHARE", "SHARE Rewards", 0xFF9A0229),
    PresetRetailer("VOX Cinemas SHARE", "SHARE Cinemas", 0xFF0554BD),
    PresetRetailer("Dubai Mall Rewards", "Dubai Mall", 0xFF151525),
    PresetRetailer("Union Coop Tamayaz", "Tamayaz", 0xFF00897B),
    PresetRetailer("Viva Club", "Viva Rewards", 0xFFD32F2F),
    PresetRetailer("Sharaf DG", "DG Member", 0xFFF57F17)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoyaltyCardsScreen(
    loyaltyCardDao: LoyaltyCardDao,
    selectedEmirate: String = "Dubai",
    hasUnreadNotifications: Boolean = false,
    onNavigateToLocation: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val palette = LocalAppPalette.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val cards by loyaltyCardDao.getAllCards().collectAsState(initial = emptyList())
    val demoEnabled by DemoRegistry.isDemoEnabled.collectAsState()
    val demoSnapshot = remember(demoEnabled) { DemoRegistry.currentSnapshot() }
    val localizedEmirate = remember(selectedEmirate, context) {
        LocationManager.localizedEmirateName(context, selectedEmirate)
    }

    LaunchedEffect(cards.isEmpty(), demoEnabled) {
        val prefs = context.getSharedPreferences("wallet_prefs", Context.MODE_PRIVATE)
        if (demoEnabled && demoSnapshot.walletSeedCards.isNotEmpty() && !prefs.getBoolean("seeded_v1", false) && cards.isEmpty()) {
            demoSnapshot.walletSeedCards.forEach { card ->
                loyaltyCardDao.insertCard(card)
            }
            prefs.edit().putBoolean("seeded_v1", true).apply()
        }
    }

    var addDialogPreset by remember { mutableStateOf<PresetRetailer?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showAllCardsDialog by remember { mutableStateOf(false) }
    var activeCardId by remember { mutableStateOf<String?>(null) }
    var selectedForScan by remember { mutableStateOf<LoyaltyCardEntity?>(null) }
    var cardToDelete by remember { mutableStateOf<LoyaltyCardEntity?>(null) }

    val orderedCards = remember(cards) { cards.sortedBy { it.createdAt } }
    val activeCard = remember(orderedCards, activeCardId) {
        orderedCards.firstOrNull { it.id == activeCardId } ?: orderedCards.firstOrNull()
    }
    val totalPoints = remember(orderedCards) {
        orderedCards.sumOf { parseCardPoints(it) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 2.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            WalletHeader(
                locationName = localizedEmirate,
                hasUnreadNotifications = hasUnreadNotifications,
                onBack = onNavigateBack,
                onNavigateToLocation = onNavigateToLocation,
                onNavigateToNotifications = onNavigateToNotifications
            )
        }

        item {
            WalletActionAndStatsRow(
                cardCount = orderedCards.size,
                totalPoints = totalPoints,
                onAddCardClick = {
                    addDialogPreset = null
                    showAddDialog = true
                },
                onCardsStatClick = { showAllCardsDialog = true },
                onReadyScanClick = {
                    if (activeCard != null) selectedForScan = activeCard
                    else {
                        addDialogPreset = null
                        showAddDialog = true
                    }
                }
            )
        }

        item {
            MyLoyaltyCardsSection(
                cards = orderedCards,
                demoPresets = demoSnapshot.walletPresets,
                onViewAll = { showAllCardsDialog = true },
                onCardClick = { card ->
                    if (activeCard?.id == card.id) {
                        selectedForScan = card
                    } else {
                        activeCardId = card.id
                    }
                },
                onScanClick = { card ->
                    activeCardId = card.id
                    selectedForScan = card
                }
            )
        }

        item {
            ScanAtCheckoutSection(
                activeCard = activeCard,
                onBarcodeClick = {
                    if (activeCard != null) {
                        selectedForScan = activeCard
                    } else {
                        addDialogPreset = null
                        showAddDialog = true
                    }
                }
            )
        }

        item {
            ExploreRewardsSection(
                demoPresets = demoSnapshot.walletPresets,
                onSelectBrand = { preset ->
                    addDialogPreset = preset
                    showAddDialog = true
                },
                onAddMoreClick = {
                    addDialogPreset = null
                    showAddDialog = true
                }
            )
        }
    }

    selectedForScan?.let { card ->
        FullscreenBarcodeDialog(
            card = card,
            onDismiss = { selectedForScan = null },
            context = context
        )
    }

    if (showAddDialog) {
        AddLoyaltyCardDialog(
            initialPreset = addDialogPreset,
            onDismiss = { showAddDialog = false },
            onSave = { newCard ->
                coroutineScope.launch {
                    loyaltyCardDao.insertCard(newCard)
                    activeCardId = newCard.id
                    Toast.makeText(context, "${newCard.storeName} card saved!", Toast.LENGTH_SHORT).show()
                }
                showAddDialog = false
            }
        )
    }

    if (showAllCardsDialog) {
        AllLoyaltyCardsDialog(
            cards = orderedCards,
            onDismiss = { showAllCardsDialog = false },
            onScan = { card ->
                showAllCardsDialog = false
                activeCardId = card.id
                selectedForScan = card
            },
            onDelete = { card -> cardToDelete = card },
            onAddNew = {
                showAllCardsDialog = false
                addDialogPreset = null
                showAddDialog = true
            }
        )
    }

    cardToDelete?.let { card ->
        AlertDialog(
            onDismissRequest = { cardToDelete = null },
            title = { Text("Delete Loyalty Card") },
            text = { Text("Remove ${card.storeName} (${card.cardNumber}) from your Wallet?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            loyaltyCardDao.deleteCardById(card.id)
                            Toast.makeText(context, "Card removed", Toast.LENGTH_SHORT).show()
                        }
                        cardToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = FigmaCrimsonLink)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { cardToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun WalletHeader(
    locationName: String,
    hasUnreadNotifications: Boolean,
    onBack: () -> Unit,
    onNavigateToLocation: () -> Unit,
    onNavigateToNotifications: () -> Unit
) {
    val palette = LocalAppPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .shadow(6.dp, CircleShape, ambientColor = Color(0x26272445), spotColor = Color(0x26272445))
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                    .border(1.dp, palette.cardBorder, CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Back",
                    tint = palette.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(R.string.loyalty_wallet_title),
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        letterSpacing = (-0.5).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.loyalty_wallet_subtitle),
                    style = TextStyle(
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = palette.textSecondary,
                        letterSpacing = (-0.25).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .height(35.dp)
                    .shadow(6.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x26272445), spotColor = Color(0x26272445))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
                    .clickable { onNavigateToLocation() }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = stringResource(R.string.location),
                        modifier = Modifier.size(16.dp),
                        tint = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = locationName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = palette.textPrimary
                    )
                }
            }

            Box(modifier = Modifier.size(36.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(6.dp, CircleShape, ambientColor = Color(0x26272445), spotColor = Color(0x26272445))
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                        .border(1.dp, palette.cardBorder, CircleShape)
                        .clickable { onNavigateToNotifications() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = stringResource(R.string.notifications),
                        modifier = Modifier.size(17.dp),
                        tint = palette.textPrimary
                    )
                }
                if (hasUnreadNotifications) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = (-2).dp, y = 1.dp)
                            .clip(CircleShape)
                            .background(FigmaCrimsonDot)
                    )
                }
            }
        }
    }
}

@Composable
private fun WalletActionAndStatsRow(
    cardCount: Int,
    totalPoints: Int,
    onAddCardClick: () -> Unit,
    onCardsStatClick: () -> Unit,
    onReadyScanClick: () -> Unit
) {
    val palette = LocalAppPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(106.dp)
                .height(44.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = Color(0x40ED0032),
                    spotColor = Color(0x40ED0032)
                )
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.verticalGradient(listOf(FigmaCrimsonStart, FigmaCrimsonEnd)))
                .border(0.8.dp, Color(0x66FF5575), RoundedCornerShape(22.dp))
                .clickable { onAddCardClick() }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = stringResource(R.string.add_card),
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-0.25).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    maxLines = 1
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .shadow(6.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x1E272445), spotColor = Color(0x1E272445))
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                .border(1.dp, palette.cardBorder, RoundedCornerShape(22.dp))
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(0.85f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onCardsStatClick() }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlineCardStatIcon(modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Column {
                        Text(
                            text = cardCount.toString(),
                            style = TextStyle(
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.textPrimary,
                                fontFeatureSettings = "tnum",
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                        Text(
                            text = stringResource(R.string.cards_label),
                            style = TextStyle(
                                fontSize = 8.2.sp,
                                color = palette.textMuted,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                    }
                }

                Box(modifier = Modifier.width(1.dp).height(22.dp).background(palette.divider))

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.StarOutline,
                        contentDescription = null,
                        tint = palette.textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = "%,d".format(totalPoints),
                            style = TextStyle(
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.textPrimary,
                                fontFeatureSettings = "tnum",
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = stringResource(R.string.points_label),
                            style = TextStyle(
                                fontSize = 8.2.sp,
                                color = palette.textMuted,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            )
                        )
                    }
                }

                Box(modifier = Modifier.width(1.dp).height(22.dp).background(palette.divider))

                Row(
                    modifier = Modifier
                        .weight(1.05f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onReadyScanClick() }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlineBoltIcon(modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.ready_to_scan),
                        style = TextStyle(
                            fontSize = 8.8.sp,
                            lineHeight = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun MyLoyaltyCardsSection(
    cards: List<LoyaltyCardEntity>,
    demoPresets: List<DemoWalletPreset>,
    onViewAll: () -> Unit,
    onCardClick: (LoyaltyCardEntity) -> Unit,
    onScanClick: (LoyaltyCardEntity) -> Unit
) {
    val palette = LocalAppPalette.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RedWalletHeaderIcon(modifier = Modifier.size(width = 17.dp, height = 14.dp))
            Spacer(modifier = Modifier.width(7.dp))
            Text(
                text = stringResource(R.string.my_loyalty_cards),
                style = TextStyle(
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    letterSpacing = (-0.4).sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onViewAll() }
            ) {
                Text(
                    text = stringResource(R.string.view_all),
                    style = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.crimsonLink,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = palette.crimsonLink,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (cards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
                    .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.empty_wallet_desc),
                    fontSize = 12.sp,
                    color = palette.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cards, key = { it.id }) { card ->
                    WalletLoyaltyShowcaseCard(
                        card = card,
                        demoPresets = demoPresets,
                        onCardClick = { onCardClick(card) },
                        onScanClick = { onScanClick(card) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WalletLoyaltyShowcaseCard(
    card: LoyaltyCardEntity,
    demoPresets: List<DemoWalletPreset>,
    onCardClick: () -> Unit,
    onScanClick: () -> Unit
) {
    val palette = LocalAppPalette.current
    val nameLower = card.storeName.lowercase()
    val pts = parseCardPoints(card)
    val ptsLabel = if (pts > 0) "%,d pts".format(pts) else "Scan ID"
    val matchingPreset = remember(card.storeName, demoPresets) {
        demoPresets.firstOrNull { p ->
            val key = p.name.substringBefore(" ").lowercase()
            nameLower.contains(key)
        }
    }

    Box(
        modifier = Modifier
            .width(122.dp)
            .shadow(5.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x1824203C), spotColor = Color(0x1824203C))
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(palette.surfaceGradStart, palette.surfaceGradMid, palette.surfaceGradEnd)))
            .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
            .clickable { onCardClick() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .padding(3.dp)
                    .clip(RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    nameLower.contains("lulu") -> LuluHappinessCardArtwork(logoUri = matchingPreset?.logoResUri)
                    nameLower.contains("nesto") || nameLower.contains("inaam") -> NestoInaamCardArtwork(logoUri = matchingPreset?.stackedLogoResUri ?: matchingPreset?.logoResUri)
                    nameLower.contains("carrefour") -> CarrefourShareCardArtwork(logoUri = matchingPreset?.logoResUri)
                    else -> GenericLoyaltyCardArtwork(card = card, logoUri = matchingPreset?.logoResUri)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 9.dp, vertical = 7.dp)
            ) {
                Text(
                    text = card.storeName,
                    style = TextStyle(
                        fontSize = 9.2.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        letterSpacing = (-0.25).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ptsLabel,
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.crimsonLink,
                            letterSpacing = (-0.2).sp,
                            fontFeatureSettings = "tnum",
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        maxLines = 1
                    )
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(palette.crimsonPillBg)
                            .clickable { onScanClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Scan",
                            tint = palette.crimsonLink,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LuluHappinessCardArtwork(logoUri: String? = null) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF7B2CBF), Color(0xFF5A189A), Color(0xFF3E0A71))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0x55A855F7), Color.Transparent),
                    center = Offset(w * 0.50f, h * 0.35f),
                    radius = w * 0.65f
                ),
                radius = w * 0.65f,
                center = Offset(w * 0.50f, h * 0.35f)
            )
            val wave = Path().apply {
                moveTo(0f, h * 0.90f)
                cubicTo(w * 0.35f, h * 0.96f, w * 0.62f, h * 0.72f, w, h * 0.78f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                wave,
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFFD99B06), Color(0xFFFDE047), Color(0xFFF59E0B))
                )
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            if (!logoUri.isNullOrBlank()) {
                AsyncImage(
                    model = logoUri,
                    contentDescription = "LuLu",
                    modifier = Modifier.height(21.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text("LuLu", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "HAPPINESS",
                style = TextStyle(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFF7C948),
                    letterSpacing = 0.5.sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = "LOYALTY REWARDED",
                style = TextStyle(
                    fontSize = 5.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.9f),
                    letterSpacing = 0.6.sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }
    }
}

@Composable
private fun NestoInaamCardArtwork(logoUri: String? = null) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1D3149), Color(0xFF101824), Color(0xFF0A0E16))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val wave = Path().apply {
                moveTo(w * 0.30f, h)
                cubicTo(w * 0.58f, h * 0.88f, w * 0.74f, h * 0.72f, w, h * 0.76f)
                lineTo(w, h)
                close()
            }
            drawPath(
                wave,
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFF0E296B), Color(0xFF2563EB), Color(0xFF1D4ED8))
                )
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            if (!logoUri.isNullOrBlank()) {
                AsyncImage(
                    model = logoUri,
                    contentDescription = "Nesto",
                    modifier = Modifier.height(31.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text("NESTO", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
            Spacer(modifier = Modifier.height(6.dp))
            InaamGoldWordmark(modifier = Modifier.size(width = 74.dp, height = 18.dp))
        }
    }
}

@Composable
private fun InaamGoldWordmark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = h * 0.13f
        val brush = Brush.verticalGradient(
            listOf(Color(0xFFFCE38A), Color(0xFFE6B94D), Color(0xFFC59326))
        )
        val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val topY = h * 0.22f
        val botY = h * 0.86f

        // I
        drawLine(brush, Offset(w * 0.06f, topY), Offset(w * 0.06f, botY), sw, StrokeCap.Round)
        // N
        val nPath = Path().apply {
            moveTo(w * 0.16f, botY)
            lineTo(w * 0.16f, topY)
            lineTo(w * 0.30f, botY)
            lineTo(w * 0.30f, topY)
        }
        drawPath(nPath, brush = brush, style = stroke)
        // Lambda 1
        val a1 = Path().apply {
            moveTo(w * 0.37f, botY)
            lineTo(w * 0.45f, topY)
            lineTo(w * 0.53f, botY)
        }
        drawPath(a1, brush = brush, style = stroke)
        // Lambda 2
        val a2 = Path().apply {
            moveTo(w * 0.57f, botY)
            lineTo(w * 0.65f, topY)
            lineTo(w * 0.73f, botY)
        }
        drawPath(a2, brush = brush, style = stroke)
        // M
        val mPath = Path().apply {
            moveTo(w * 0.79f, botY)
            lineTo(w * 0.79f, topY)
            lineTo(w * 0.88f, h * 0.62f)
            lineTo(w * 0.96f, topY)
            lineTo(w * 0.96f, botY)
        }
        drawPath(mPath, brush = brush, style = stroke)

        // Small 4-point sparkle above right shoulder of Lambda 2 / M
        val cx = w * 0.88f
        val cy = h * 0.16f
        val r = h * 0.17f
        val star = Path().apply {
            moveTo(cx, cy - r)
            lineTo(cx + r * 0.30f, cy - r * 0.30f)
            lineTo(cx + r, cy)
            lineTo(cx + r * 0.30f, cy + r * 0.30f)
            lineTo(cx, cy + r)
            lineTo(cx - r * 0.30f, cy + r * 0.30f)
            lineTo(cx - r, cy)
            lineTo(cx - r * 0.30f, cy - r * 0.30f)
            close()
        }
        drawPath(star, color = Color(0xFFFDE047))
    }
}

@Composable
private fun CarrefourShareCardArtwork(logoUri: String? = null) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFC90035), Color(0xFF8C0023), Color(0xFF540014))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val wave = Path().apply {
                moveTo(0f, h * 0.94f)
                cubicTo(w * 0.32f, h * 0.88f, w * 0.56f, h * 0.68f, w, h * 0.74f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                wave,
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFF8C001F), Color(0xFFE6002E), Color(0xFFFF1F4D))
                )
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            if (!logoUri.isNullOrBlank()) {
                AsyncImage(
                    model = logoUri,
                    contentDescription = "Carrefour",
                    modifier = Modifier.height(30.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text("Carrefour", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "SHARE",
                style = TextStyle(
                    fontSize = 17.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White,
                    letterSpacing = 1.1.sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }
    }
}

@Composable
private fun GenericLoyaltyCardArtwork(card: LoyaltyCardEntity, logoUri: String? = null) {
    val base = Color(card.colorHex)
    val nameLower = card.storeName.lowercase()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(base, base.copy(alpha = 0.82f), Color(0xFF0F172A).copy(alpha = 0.85f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val wave = Path().apply {
                moveTo(0f, h * 0.92f)
                cubicTo(w * 0.40f, h * 0.85f, w * 0.65f, h * 0.70f, w, h * 0.75f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(wave, color = Color.White.copy(alpha = 0.16f))
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            if (nameLower.contains("vox")) {
                if (!logoUri.isNullOrBlank()) {
                    AsyncImage(
                        model = logoUri,
                        contentDescription = "Majid Al Futtaim SHARE",
                        modifier = Modifier.height(22.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text("VOX", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White)
                Text("SHARE CINEMAS", fontSize = 6.5.sp, color = Color.White.copy(alpha = 0.85f), letterSpacing = 0.8.sp)
            } else {
                if (!logoUri.isNullOrBlank()) {
                    AsyncImage(
                        model = logoUri,
                        contentDescription = card.storeName,
                        modifier = Modifier.height(24.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = card.storeName.split(" ").firstOrNull()?.uppercase() ?: card.storeName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "REWARDS",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.85f),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun ScanAtCheckoutSection(
    activeCard: LoyaltyCardEntity?,
    onBarcodeClick: () -> Unit
) {
    val palette = LocalAppPalette.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .shadow(6.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x1824203C), spotColor = Color(0x1824203C))
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (palette.isDark) {
                    Brush.horizontalGradient(listOf(Color(0xFF281722), Color(0xFF331B29)))
                } else {
                    Brush.horizontalGradient(listOf(Color(0xFFFEF7FC), Color(0xFFFDF0F6), Color(0xFFFCD9E5)))
                }
            )
            .border(1.dp, palette.cardBorder, RoundedCornerShape(22.dp))
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0x55F9A8BF), Color.Transparent),
                    center = Offset(size.width * 0.88f, size.height * 0.15f),
                    radius = size.width * 0.55f
                ),
                radius = size.width * 0.55f,
                center = Offset(size.width * 0.88f, size.height * 0.15f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RedBarcodeBarsIcon(modifier = Modifier.size(width = 18.dp, height = 14.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.scan_at_checkout),
                    style = TextStyle(
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        letterSpacing = (-0.4).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
                    .shadow(3.dp, RoundedCornerShape(14.dp), ambientColor = Color(0x1424203C), spotColor = Color(0x1424203C))
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .clickable { onBarcodeClick() }
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (activeCard != null) {
                    BarcodeView(
                        data = activeCard.cardNumber,
                        format = activeCard.barcodeFormat,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = stringResource(R.string.add_card_to_scan),
                        style = TextStyle(
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FigmaTextMuted,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(9.dp))

            Text(
                text = if (activeCard != null) {
                    "${activeCard.storeName} • ${stringResource(R.string.open_card_to_scan)}"
                } else {
                    stringResource(R.string.open_card_to_scan)
                },
                style = TextStyle(
                    fontSize = 10.5.sp,
                    color = palette.textSecondary,
                    letterSpacing = (-0.2).sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ExploreRewardsSection(
    demoPresets: List<DemoWalletPreset>,
    onSelectBrand: (PresetRetailer) -> Unit,
    onAddMoreClick: () -> Unit
) {
    val palette = LocalAppPalette.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RedGiftBoxIcon(modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(R.string.explore_rewards),
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        letterSpacing = (-0.4).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.explore_rewards_desc),
                    style = TextStyle(
                        fontSize = 10.5.sp,
                        color = palette.textSecondary,
                        letterSpacing = (-0.2).sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            val tiles = if (demoPresets.isNotEmpty()) {
                demoPresets.take(5)
            } else {
                UAE_PRESET_RETAILERS.take(5).mapIndexed { idx, p ->
                    DemoWalletPreset(
                        id = "fallback_$idx",
                        name = p.name,
                        subtitle = p.name.substringBefore(" "),
                        defaultPoints = "",
                        defaultCardNumber = "",
                        logoResUri = "",
                        stackedLogoResUri = null,
                        cardBgColorHex = p.colorHex,
                        secondaryBgColorHex = p.colorHex,
                        accentColorHex = 0xFFFFFFFF
                    )
                }
            }

            tiles.forEach { preset ->
                ExploreBrandTile(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onSelectBrand(
                            PresetRetailer(
                                name = preset.name,
                                programName = preset.badgeTitle.ifBlank { "Rewards" },
                                colorHex = preset.cardBgColorHex
                            )
                        )
                    }
                ) {
                    val uri = preset.stackedLogoResUri ?: preset.logoResUri
                    if (preset.name.contains("vox", ignoreCase = true) && uri.isNotBlank()) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = preset.name,
                                modifier = Modifier.height(15.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("VOX", fontSize = 11.sp, fontWeight = FontWeight.Black, color = FigmaTextPrimary, style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)))
                            Text("CINEMAS", fontSize = 4.6.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = FigmaTextPrimary, style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)))
                        }
                    } else if (uri.isNotBlank()) {
                        AsyncImage(
                            model = uri,
                            contentDescription = preset.name,
                            modifier = Modifier.fillMaxSize().padding(6.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        val lines = preset.subtitle.split("\n")
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = lines.firstOrNull().orEmpty(),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FigmaTextPrimary,
                                maxLines = 1,
                                style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                            )
                            if (lines.size > 1) {
                                Text(
                                    text = lines[1],
                                    fontSize = 8.2.sp,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 0.9.sp,
                                    color = FigmaTextPrimary,
                                    maxLines = 1,
                                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false))
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
                    .shadow(3.dp, RoundedCornerShape(14.dp), ambientColor = Color(0x1424203C), spotColor = Color(0x1424203C))
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFF4F8), Color(0xFFFDE3ED))))
                    .border(1.dp, Color(0xFFFBCFE0), RoundedCornerShape(14.dp))
                    .clickable { onAddMoreClick() },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_more),
                        tint = FigmaCrimsonText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.add_more),
                        style = TextStyle(
                            fontSize = 7.6.sp,
                            fontWeight = FontWeight.Bold,
                            color = FigmaCrimsonText,
                            letterSpacing = (-0.2).sp,
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ExploreBrandTile(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .height(58.dp)
            .shadow(4.dp, RoundedCornerShape(14.dp), ambientColor = Color(0x1424203C), spotColor = Color(0x1424203C))
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, Color.White, RoundedCornerShape(14.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
private fun OutlineCardStatIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.11f
        val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawRoundRect(
            color = FigmaTextPrimary,
            topLeft = Offset(w * 0.08f, h * 0.16f),
            size = Size(w * 0.84f, h * 0.68f),
            cornerRadius = CornerRadius(w * 0.14f, w * 0.14f),
            style = stroke
        )
        drawLine(
            color = FigmaTextPrimary,
            start = Offset(w * 0.08f, h * 0.38f),
            end = Offset(w * 0.92f, h * 0.38f),
            strokeWidth = sw
        )
    }
}

@Composable
private fun OutlineBoltIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.11f
        val p = Path().apply {
            moveTo(w * 0.56f, h * 0.08f)
            lineTo(w * 0.18f, h * 0.54f)
            lineTo(w * 0.50f, h * 0.54f)
            lineTo(w * 0.44f, h * 0.92f)
            lineTo(w * 0.82f, h * 0.46f)
            lineTo(w * 0.50f, h * 0.46f)
            close()
        }
        drawPath(p, color = FigmaTextPrimary, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun RedWalletHeaderIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = FigmaCrimsonLink,
            topLeft = Offset(0f, h * 0.08f),
            size = Size(w * 0.96f, h * 0.88f),
            cornerRadius = CornerRadius(h * 0.22f, h * 0.22f)
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(w * 0.64f, h * 0.38f),
            size = Size(w * 0.36f, h * 0.30f),
            cornerRadius = CornerRadius(h * 0.10f, h * 0.10f)
        )
        drawCircle(
            color = FigmaCrimsonLink,
            radius = h * 0.07f,
            center = Offset(w * 0.78f, h * 0.53f)
        )
    }
}

@Composable
private fun RedBarcodeBarsIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val bars = listOf(
            0.00f to 0.14f,
            0.22f to 0.09f,
            0.40f to 0.16f,
            0.66f to 0.09f,
            0.84f to 0.16f
        )
        bars.forEach { (x, bw) ->
            drawRoundRect(
                color = FigmaCrimsonLink,
                topLeft = Offset(w * x, 0f),
                size = Size(w * bw, h),
                cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
            )
        }
    }
}

@Composable
private fun RedGiftBoxIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.09f
        // Lid
        drawRoundRect(
            color = FigmaCrimsonLink,
            topLeft = Offset(w * 0.08f, h * 0.28f),
            size = Size(w * 0.84f, h * 0.20f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
        )
        // Base left & right halves
        drawRoundRect(
            color = FigmaCrimsonLink,
            topLeft = Offset(w * 0.14f, h * 0.54f),
            size = Size(w * 0.31f, h * 0.40f),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
        )
        drawRoundRect(
            color = FigmaCrimsonLink,
            topLeft = Offset(w * 0.55f, h * 0.54f),
            size = Size(w * 0.31f, h * 0.40f),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
        )
        // Ribbon loops
        drawOval(
            color = FigmaCrimsonLink,
            topLeft = Offset(w * 0.20f, h * 0.08f),
            size = Size(w * 0.28f, h * 0.20f),
            style = Stroke(width = sw)
        )
        drawOval(
            color = FigmaCrimsonLink,
            topLeft = Offset(w * 0.52f, h * 0.08f),
            size = Size(w * 0.28f, h * 0.20f),
            style = Stroke(width = sw)
        )
    }
}

@Composable
private fun AllLoyaltyCardsDialog(
    cards: List<LoyaltyCardEntity>,
    onDismiss: () -> Unit,
    onScan: (LoyaltyCardEntity) -> Unit,
    onDelete: (LoyaltyCardEntity) -> Unit,
    onAddNew: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "All Loyalty Cards (${cards.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = FigmaTextPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = FigmaTextPrimary)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    cards.forEach { card ->
                        val pts = parseCardPoints(card)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(card.colorHex).copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Color(card.colorHex).copy(alpha = 0.25f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onScan(card) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color(card.colorHex))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = card.storeName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = FigmaTextPrimary
                                    )
                                    Text(
                                        text = if (pts > 0) "${formatCardNumber(card.cardNumber)} • %,d pts".format(pts)
                                        else formatCardNumber(card.cardNumber),
                                        fontSize = 11.sp,
                                        color = FigmaTextSecondary
                                    )
                                }
                                IconButton(onClick = { onDelete(card) }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = FigmaCrimsonLink, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onAddNew,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FigmaCrimsonEnd)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add New Card")
                }
            }
        }
    }
}

@Composable
fun FullscreenBarcodeDialog(
    card: LoyaltyCardEntity,
    onDismiss: () -> Unit,
    context: Context
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            card.storeName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(card.colorHex)
                        )
                        Text(
                            "Loyalty & Rewards Barcode",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Black)
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(2.dp, Color.LightGray.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            BarcodeView(
                                data = card.cardNumber,
                                format = card.barcodeFormat,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (card.barcodeFormat == "QR_CODE") 220.dp else 140.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = card.cardNumber,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 3.sp,
                                color = Color.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.BrightnessHigh,
                            contentDescription = null,
                            tint = Color.DarkGray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Turn screen brightness up for easiest scanner reading",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.DarkGray
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Loyalty Card", card.cardNumber)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Card number copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy Number")
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(card.colorHex))
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoyaltyCardDialog(
    initialPreset: PresetRetailer? = null,
    onDismiss: () -> Unit,
    onSave: (LoyaltyCardEntity) -> Unit
) {
    var storeName by remember(initialPreset) { mutableStateOf(initialPreset?.name ?: "") }
    var cardNumber by remember { mutableStateOf("") }
    var pointsText by remember { mutableStateOf("") }
    var selectedColor by remember(initialPreset) { mutableStateOf(initialPreset?.colorHex ?: 0xFF9A0229) }
    var barcodeFormat by remember(initialPreset) { mutableStateOf(initialPreset?.defaultFormat ?: "CODE_128") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Loyalty Card", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "UAE Rewards & Malls:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(UAE_PRESET_RETAILERS) { preset ->
                        FilterChip(
                            selected = storeName == preset.name,
                            onClick = {
                                storeName = preset.name
                                selectedColor = preset.colorHex
                                barcodeFormat = preset.defaultFormat
                            },
                            label = { Text(preset.name, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(preset.colorHex),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("Card / Store Name *") },
                    placeholder = { Text("e.g. LuLu Happiness, Nesto INAAM") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { cardNumber = it },
                    label = { Text("Card Number / Barcode *") },
                    placeholder = { Text("e.g. 971504123456") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Barcode Format:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("CODE_128" to "Code 128 (1D)", "QR_CODE" to "QR Code (2D)").forEach { (fmt, label) ->
                        FilterChip(
                            selected = barcodeFormat == fmt,
                            onClick = { barcodeFormat = fmt },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = pointsText,
                    onValueChange = { pointsText = it },
                    label = { Text("Points Balance (Optional)") },
                    placeholder = { Text("e.g. 500") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (storeName.isNotBlank() && cardNumber.isNotBlank()) {
                        onSave(
                            LoyaltyCardEntity(
                                storeName = storeName.trim(),
                                cardNumber = cardNumber.trim(),
                                barcodeFormat = barcodeFormat,
                                colorHex = selectedColor,
                                note = pointsText.trim()
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FigmaCrimsonEnd),
                enabled = storeName.isNotBlank() && cardNumber.isNotBlank()
            ) { Text("Save Card") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun parseCardPoints(card: LoyaltyCardEntity): Int {
    return card.note.filter { it.isDigit() }.toIntOrNull() ?: 0
}

private fun formatCardNumber(num: String): String {
    return num.chunked(4).joinToString(" ")
}
