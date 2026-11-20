package com.example.ui.flyers

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlyerViewerScreen(flyerId: String, viewModel: com.example.ui.DealFinderViewModel, onBack: () -> Unit) {
    val isLoaded by viewModel.isLoaded.collectAsState()
    val flyers by viewModel.flyers.collectAsState()
    val savedFlyers by viewModel.savedFlyers.collectAsState()
    val flyer = flyers.find { it.id == flyerId }
    val isSaved = flyer?.let { f -> savedFlyers.any { it.id == f.id } } ?: false

    if (!isLoaded) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Loading Flyer...") },
                    navigationIcon = {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }

    if (flyer == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Not Found") },
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
                    Text("Flyer not found or expired.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("This catalog flyer may have ended its promotional offer period or is currently unpublished.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onBack) { Text("Go Back") }
                }
            }
        }
        return
    }

    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    var cachedPdfFile by remember(flyer.id, flyer.pdfUrl) { mutableStateOf<File?>(null) }
    var pdfPageCount by remember(flyer.id) { mutableIntStateOf(0) }
    var currentPageIndex by remember(flyer.id) { mutableIntStateOf(0) }
    var renderedPageBitmap by remember(flyer.id) { mutableStateOf<ImageBitmap?>(null) }
    var isPdfLoading by remember(flyer.id) { mutableStateOf(false) }

    // Download and cache full PDF locally so multi-page catalogues render cleanly offline too
    LaunchedEffect(flyer.id, flyer.pdfUrl) {
        if (!flyer.pdfUrl.startsWith("http")) return@LaunchedEffect
        isPdfLoading = true
        val localFile = withContext(Dispatchers.IO) {
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
                        target.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                if (target.exists() && target.length() > 0L) target else null
            } catch (_: Exception) {
                null
            }
        }
        cachedPdfFile = localFile
        isPdfLoading = false
    }

    // Render active PDF page using native Android PdfRenderer
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
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        flyer.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (flyer.pdfUrl.isNotBlank()) {
                        IconButton(onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                val uri = if (flyer.pdfUrl.startsWith("http")) {
                                    android.net.Uri.parse(flyer.pdfUrl)
                                } else {
                                    android.net.Uri.parse(flyer.imageUrl)
                                }
                                setDataAndType(uri, "application/pdf")
                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                try {
                                    val webIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(flyer.pdfUrl.ifBlank { flyer.imageUrl }))
                                    context.startActivity(webIntent)
                                } catch (ex: Exception) {
                                    android.widget.Toast.makeText(context, "Cannot open PDF viewer", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = "Open Full Catalogue PDF")
                        }
                    }
                    IconButton(onClick = { viewModel.toggleSavedFlyer(flyer) }) {
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isSaved) "Remove from Saved" else "Save Flyer",
                            tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = {
                        val shareIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, "Check out this flyer: ${flyer.title}")
                            type = "text/plain"
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share flyer"))
                    }) {
                        Icon(Icons.Default.Share, "Share Flyer")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 3f)
                        offsetX += pan.x * scale
                        offsetY += pan.y * scale
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
                    model = flyer.imageUrl.ifBlank { fallbackFlyerUrl },
                    error = coil.compose.rememberAsyncImagePainter(fallbackFlyerUrl),
                    contentDescription = flyer.title,
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
            }

            if (isPdfLoading && activeBitmap == null) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }

            if (pdfPageCount > 1) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
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
    }
}
