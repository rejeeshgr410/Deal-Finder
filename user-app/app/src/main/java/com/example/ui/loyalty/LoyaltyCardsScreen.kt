package com.example.ui.loyalty

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.LoyaltyCardDao
import com.example.data.LoyaltyCardEntity
import kotlinx.coroutines.launch

data class PresetRetailer(
    val name: String,
    val programName: String,
    val colorHex: Long,
    val defaultFormat: String = "CODE_128"
)

val UAE_PRESET_RETAILERS = listOf(
    PresetRetailer("Carrefour", "MyClub", 0xFF004F9F),
    PresetRetailer("Lulu Hypermarket", "Happiness", 0xFF007A3D),
    PresetRetailer("Nesto", "INAM Rewards", 0xFFE65100),
    PresetRetailer("Union Coop", "Tamayaz", 0xFF00897B),
    PresetRetailer("Air Miles", "Air Miles UAE", 0xFF0D47A1),
    PresetRetailer("Viva", "Viva Club", 0xFFD32F2F),
    PresetRetailer("Safari", "Safari Club", 0xFFB71C1C),
    PresetRetailer("Sharaf DG", "DG Shield", 0xFFF57F17),
    PresetRetailer("IKEA", "IKEA Family", 0xFF0058A9)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoyaltyCardsScreen(
    loyaltyCardDao: LoyaltyCardDao,
    onNavigateBack: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val cards by loyaltyCardDao.getAllCards().collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedForScan by remember { mutableStateOf<LoyaltyCardEntity?>(null) }
    var cardToDelete by remember { mutableStateOf<LoyaltyCardEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Loyalty Cards Wallet",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "Scan digital barcodes at cashier checkouts",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Card")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (cards.isEmpty()) {
            EmptyLoyaltyState(
                onAddClicked = { showAddDialog = true },
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Tap any membership card to display high-contrast barcode for supermarket cashiers.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                items(cards, key = { it.id }) { card ->
                    LoyaltyCardItem(
                        card = card,
                        onScanClicked = { selectedForScan = card },
                        onDeleteClicked = { cardToDelete = card }
                    )
                }
            }
        }
    }

    // Fullscreen Barcode Modal
    selectedForScan?.let { card ->
        FullscreenBarcodeDialog(
            card = card,
            onDismiss = { selectedForScan = null },
            context = context
        )
    }

    // Add Card Dialog
    if (showAddDialog) {
        AddLoyaltyCardDialog(
            onDismiss = { showAddDialog = false },
            onSave = { newCard ->
                coroutineScope.launch {
                    loyaltyCardDao.insertCard(newCard)
                    Toast.makeText(context, "${newCard.storeName} card saved!", Toast.LENGTH_SHORT).show()
                }
                showAddDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    cardToDelete?.let { card ->
        AlertDialog(
            onDismissRequest = { cardToDelete = null },
            title = { Text("Delete Loyalty Card") },
            text = { Text("Are you sure you want to remove ${card.storeName} (${card.cardNumber}) from your wallet?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            loyaltyCardDao.deleteCardById(card.id)
                            Toast.makeText(context, "Card removed", Toast.LENGTH_SHORT).show()
                        }
                        cardToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { cardToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun LoyaltyCardItem(
    card: LoyaltyCardEntity,
    onScanClicked: () -> Unit,
    onDeleteClicked: () -> Unit
) {
    val cardBg = Color(card.colorHex)
    val cardGradient = Brush.horizontalGradient(
        listOf(cardBg, cardBg.copy(alpha = 0.8f), cardBg.copy(alpha = 0.95f))
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onScanClicked),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .background(cardGradient)
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = card.storeName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (card.note.isNotBlank()) {
                            Text(
                                text = card.note,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDeleteClicked,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Card Number display
                Text(
                    text = formatCardNumber(card.cardNumber),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Barcode Preview Chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BarcodeView(
                            data = card.cardNumber,
                            format = card.barcodeFormat,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(end = 12.dp)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "TAP TO EXPAND",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(card.colorHex)
                            )
                            Icon(
                                Icons.Default.OpenInFull,
                                contentDescription = null,
                                tint = Color(card.colorHex),
                                modifier = Modifier.size(14.dp).padding(start = 2.dp)
                            )
                        }
                    }
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
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(24.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top close bar
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
                            "Supermarket Loyalty Card",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Black)
                    }
                }

                // Giant Barcode Area
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 24.dp)
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

                // Bottom Action buttons
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
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy Number")
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(50.dp),
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
    onDismiss: () -> Unit,
    onSave: (LoyaltyCardEntity) -> Unit
) {
    var storeName by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(0xFF004F9F) }
    var barcodeFormat by remember { mutableStateOf("CODE_128") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Loyalty Card") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Quick Pick UAE Retailer:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Presets row
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

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("Store Name *") },
                    placeholder = { Text("e.g. Carrefour, Lulu") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { cardNumber = it },
                    label = { Text("Card Number / Barcode *") },
                    placeholder = { Text("e.g. 971004123456") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Barcode Format:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
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

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (Optional)") },
                    placeholder = { Text("e.g. Family Card, Tier") },
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
                                note = note.trim()
                            )
                        )
                    }
                },
                enabled = storeName.isNotBlank() && cardNumber.isNotBlank()
            ) { Text("Save Card") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EmptyLoyaltyState(
    onAddClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(90.dp)
            ) {
                Icon(
                    Icons.Outlined.CreditCard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "Your Wallet is Empty",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Add your Carrefour MyClub, Lulu Happiness, Nesto INAM, or any retail loyalty card. Show the digital barcode at the checkout counter to earn points without plastic cards.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAddClicked,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Your First Card")
            }
        }
    }
}

private fun formatCardNumber(num: String): String {
    return num.chunked(4).joinToString(" ")
}
