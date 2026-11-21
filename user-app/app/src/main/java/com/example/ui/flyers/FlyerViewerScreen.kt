package com.example.ui.flyers

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.DealFinderViewModel
import com.example.ui.theme.FigmaCrimsonEnd
import com.example.ui.theme.FigmaCrimsonLink
import com.example.ui.theme.LocalAppPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlyerViewerScreen(
    flyerId: String,
    viewModel: DealFinderViewModel,
    isSignedIn: Boolean = false,
    onNavigateToAuth: () -> Unit = {},
    onNavigateToRetailer: (String) -> Unit = {},
    onNavigateToProduct: (String) -> Unit = {},
    onBack: () -> Unit
) {
    val palette = LocalAppPalette.current
    val context = LocalContext.current
    val isLoaded by viewModel.isLoaded.collectAsState()
    val flyers by viewModel.flyers.collectAsState()
    val products by viewModel.products.collectAsState()
    val savedFlyers by viewModel.savedFlyers.collectAsState()
    val flyer = flyers.find { it.id == flyerId }
    val store = flyer?.let { viewModel.getStore(it.retailerId) }
    val isSaved = flyer?.let { f -> savedFlyers.any { it.id == f.id } } ?: false

    if (!isLoaded && flyer == null) {
        Scaffold(
            containerColor = palette.background,
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.nav_deals)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FigmaCrimsonLink)
            }
        }
        return
    }

    if (flyer == null) {
        Scaffold(
            containerColor = palette.background,
            topBar = {
                TopAppBar(
                    title = { Text("Flyer") },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Flyer not found or expired.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "This catalog flyer may have ended its offer period or is currently unpublished.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = FigmaCrimsonEnd)) {
                        Text("Go Back", color = Color.White)
                    }
                }
            }
        }
        return
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    var cachedPdfFile by remember(flyer.id, flyer.pdfUrl) { mutableStateOf<File?>(null) }
    var pdfPageCount by remember(flyer.id) { mutableIntStateOf(0) }
    var currentPageIndex by remember(flyer.id) { mutableIntStateOf(0) }
    var renderedPageBitmap by remember(flyer.id) { mutableStateOf<ImageBitmap?>(null) }
    var isPdfLoading by remember(flyer.id) { mutableStateOf(false) }
    var pdfError by remember(flyer.id) { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(flyer.id, flyer.pdfUrl, reloadKey) {
        if (!flyer.pdfUrl.startsWith("http", ignoreCase = true)) return@LaunchedEffect
        isPdfLoading = true
        pdfError = null
        val (localFile, err) = withContext(Dispatchers.IO) {
            try {
                val dir = File(context.cacheDir, "flyers").apply { mkdirs() }
                val safeName = flyer.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                val target = File(dir, "$safeName.pdf")
                if (!target.exists() || target.length() == 0L) {
                    val conn = (URL(flyer.pdfUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15000
                        readTimeout = 30000
                    }
                    conn.inputStream.use { input ->
                        target.outputStream().use { output -> input.copyTo(output) }
                    }
                }
                if (target.exists() && target.length() > 0L) target to null
                else null to "Empty PDF file"
            } catch (e: Exception) {
                null to (e.localizedMessage ?: "Failed to load catalog PDF")
            }
        }
        cachedPdfFile = localFile
        pdfError = err
        isPdfLoading = false
    }

    LaunchedEffect(cachedPdfFile, currentPageIndex) {
        val file = cachedPdfFile ?: return@LaunchedEffect
        val (count, bmp) = withContext(Dispatchers.IO) {
            try {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        val count = renderer.pageCount
                        if (count <= 0) return@use 0 to null
                        val safeIdx = currentPageIndex.coerceIn(0, count - 1)
                        renderer.openPage(safeIdx).use { page ->
                            val targetWidth = 1080
                            val aspect = page.height.toFloat() / page.width.toFloat().coerceAtLeast(1f)
                            val targetHeight = (targetWidth * aspect).toInt().coerceAtLeast(1)
                            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                            Canvas(bitmap).drawColor(AndroidColor.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            count to bitmap.asImageBitmap()
                        }
                    }
                }
            } catch (_: Exception) {
                0 to null
            }
        }
        pdfPageCount = count
        if (bmp != null) {
            renderedPageBitmap = bmp
            scale = 1f
            offsetX = 0f
            offsetY = 0f
        } else if (count == 0) {
            pdfError = "Could not render PDF pages."
        }
    }

    val flyerDeals = remember(products, flyer.id, flyer.retailerId) {
        val exact = products.filter { it.flyerId == flyer.id }
        if (exact.isNotEmpty()) exact else products.filter { it.retailerId == flyer.retailerId }
    }

    Scaffold(
        containerColor = palette.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(flyer.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                        if (store != null) {
                            Text(store.name, fontSize = 11.sp, color = palette.crimsonLink, maxLines = 1)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (flyer.pdfUrl.startsWith("http", ignoreCase = true)) {
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(Uri.parse(flyer.pdfUrl), "application/pdf")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(flyer.pdfUrl)))
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Cannot open PDF viewer", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = "Open Full Catalogue PDF")
                        }
                    }
                    IconButton(onClick = {
                        if (!isSignedIn) {
                            Toast.makeText(context, "Sign in to bookmark flyers", Toast.LENGTH_SHORT).show()
                            onNavigateToAuth()
                        } else {
                            viewModel.toggleSavedFlyer(flyer)
                        }
                    }) {
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isSaved) "Remove from Saved" else "Save Flyer",
                            tint = if (isSaved) FigmaCrimsonLink else palette.textPrimary
                        )
                    }
                    IconButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_TEXT, "Check out this flyer: ${flyer.title}")
                            type = "text/plain"
                        }
                        try {
                            context.startActivity(Intent.createChooser(shareIntent, "Share flyer"))
                        } catch (_: Exception) {}
                    }) {
                        Icon(Icons.Default.Share, "Share Flyer")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (pdfError != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(pdfError ?: "", color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { reloadKey++ }) {
                            Text(stringResource(R.string.retry), color = FigmaCrimsonLink, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onSizeChanged { containerSize = it }
                    .pointerInput(containerSize) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 3f)
                            scale = newScale
                            if (newScale <= 1.01f) {
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                val maxX = (containerSize.width * (newScale - 1f)) / 2f
                                val maxY = (containerSize.height * (newScale - 1f)) / 2f
                                offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                                offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                            }
                        }
                    }
            ) {
                val activeBitmap = renderedPageBitmap
                if (activeBitmap != null) {
                    Image(
                        bitmap = activeBitmap,
                        contentDescription = "${flyer.title} page ${currentPageIndex + 1}",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            ),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    val fallbackFlyerUrl = "file:///android_asset/flyers/default_flyer.jpg"
                    AsyncImage(
                        model = flyer.imageUrl.ifBlank { store?.logoUrl ?: fallbackFlyerUrl },
                        error = coil.compose.rememberAsyncImagePainter(fallbackFlyerUrl),
                        contentDescription = flyer.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            ),
                        contentScale = ContentScale.Fit
                    )
                }

                if (isPdfLoading && activeBitmap == null) {
                    LinearProgressIndicator(
                        color = FigmaCrimsonLink,
                        modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                    )
                }

                if (pdfPageCount > 1) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = palette.surfaceGradStart.copy(alpha = 0.94f),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            IconButton(
                                onClick = { if (currentPageIndex > 0) currentPageIndex-- },
                                enabled = currentPageIndex > 0
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Page")
                            }
                            Text(
                                text = "Page ${currentPageIndex + 1} / $pdfPageCount",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            IconButton(
                                onClick = { if (currentPageIndex < pdfPageCount - 1) currentPageIndex++ },
                                enabled = currentPageIndex < pdfPageCount - 1
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Page")
                            }
                        }
                    }
                }
            }

            if (flyerDeals.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    color = palette.surfaceGradStart,
                    border = BorderStroke(1.dp, palette.cardBorder),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Featured Flyer Items (${flyerDeals.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = palette.textPrimary
                            )
                            if (store != null) {
                                Text(
                                    text = stringResource(R.string.view_all),
                                    color = palette.crimsonLink,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    modifier = Modifier.clickable { onNavigateToRetailer(store.id) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(flyerDeals.take(6), key = { it.id }) { deal ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(palette.subtleSurface)
                                        .clickable { onNavigateToProduct(deal.id) }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = deal.name,
                                        color = palette.textPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${stringResource(R.string.currency_aed)} %.2f".format(deal.currentPrice),
                                        color = palette.crimsonLink,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
