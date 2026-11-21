package com.example.ui.location

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LocationManager
import com.example.data.LocationStatusKind
import com.example.data.uaeEmirates
import com.example.R
import com.example.ui.components.HomeTopBackgroundWash
import com.example.ui.theme.CatGreen
import com.example.ui.theme.CatGreenIcon
import com.example.ui.theme.FigmaCrimsonEnd
import com.example.ui.theme.FigmaCrimsonStart
import com.example.ui.theme.LocalAppPalette
import kotlinx.coroutines.launch

@Composable
fun LocationScreen(locationManager: LocationManager, onBack: () -> Unit) {
    val palette = LocalAppPalette.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val selectedEmirate by locationManager.selectedEmirate.collectAsState()
    val statusState by locationManager.statusState.collectAsState()
    var localError by remember { mutableStateOf<String?>(null) }

    val isLoading = statusState.kind == LocationStatusKind.LOADING

    val arabicNames = remember {
        mapOf(
            "Dubai" to "دبي",
            "Abu Dhabi" to "أبوظبي",
            "Sharjah" to "الشارقة",
            "Ajman" to "عجمان",
            "Ras Al Khaimah" to "رأس الخيمة",
            "Fujairah" to "الفجيرة",
            "Umm Al Quwain" to "أم القيوين"
        )
    }

    val locationPermissionRequest = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
        if (granted) {
            localError = null
            coroutineScope.launch {
                val result = locationManager.fetchCurrentLocation(updateEmirateAutomatically = true)
                if (result.message.isNotBlank()) {
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            val deniedMsg = context.getString(R.string.location_permission_denied)
            localError = deniedMsg
            Toast.makeText(context, deniedMsg, Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
    ) {
        HomeTopBackgroundWash(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .align(Alignment.TopCenter)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = palette.surfaceGradStart,
                    border = BorderStroke(1.dp, palette.cardBorder),
                    modifier = Modifier
                        .size(40.dp)
                        .shadow(4.dp, CircleShape, clip = false)
                        .clip(CircleShape)
                        .clickable { onBack() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.close),
                            tint = palette.textPrimary
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.location_title),
                    color = palette.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Active Emirate Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (palette.isDark) CatGreenIcon.copy(alpha = 0.2f) else CatGreen,
                    modifier = Modifier.size(74.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = CatGreenIcon
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.active_emirate),
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.textMuted
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = LocationManager.localizedEmirateName(context, selectedEmirate),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.crimsonLink
                )
            }

            // Status Banner (GPS result or permission denied)
            val displayKind = when {
                localError != null -> LocationStatusKind.ERROR
                statusState.kind != LocationStatusKind.NONE -> statusState.kind
                else -> LocationStatusKind.NONE
            }
            val displayMsg = localError ?: statusState.message
            if (displayKind != LocationStatusKind.NONE && displayMsg.isNotBlank()) {
                val (bgColor, tintColor, icon) = when (displayKind) {
                    LocationStatusKind.ERROR -> Triple(
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                        MaterialTheme.colorScheme.error,
                        Icons.Default.ErrorOutline
                    )
                    LocationStatusKind.WARNING -> Triple(
                        Color(0xFFFEF3C7).copy(alpha = if (palette.isDark) 0.18f else 0.85f),
                        Color(0xFFD97706),
                        Icons.Default.WarningAmber
                    )
                    LocationStatusKind.SUCCESS -> Triple(
                        CatGreen.copy(alpha = if (palette.isDark) 0.18f else 0.85f),
                        CatGreenIcon,
                        Icons.Default.CheckCircle
                    )
                    LocationStatusKind.LOADING -> Triple(
                        palette.surfaceGradStart,
                        palette.crimsonLink,
                        Icons.Default.MyLocation
                    )
                    LocationStatusKind.NONE -> Triple(
                        palette.surfaceGradStart,
                        palette.crimsonLink,
                        Icons.Default.Info
                    )
                }
                Surface(
                    color = bgColor,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, tintColor.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tintColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isLoading) stringResource(R.string.detecting_gps) else displayMsg,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (palette.isDark && displayKind != LocationStatusKind.LOADING) palette.textPrimary else tintColor
                        )
                    }
                }
            }

            // Detect GPS Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 6.dp)
                    .height(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            if (isLoading) {
                                listOf(palette.subtleSurface, palette.subtleSurface)
                            } else {
                                listOf(FigmaCrimsonStart, FigmaCrimsonEnd)
                            }
                        )
                    )
                    .clickable(enabled = !isLoading) {
                        localError = null
                        locationPermissionRequest.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = palette.crimsonLink
                        )
                        Text(
                            text = stringResource(R.string.detecting_gps),
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.detect_gps),
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Guidance banner
            Surface(
                color = palette.surfaceGradStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, palette.cardBorder)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = palette.crimsonLink,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.location_guidance),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.choose_emirate),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uaeEmirates, key = { it.name }) { emirate ->
                    val isSelected = emirate.name == selectedEmirate
                    val localizedPrimary = LocationManager.localizedEmirateName(context, emirate.name)
                    val secondaryName = if (localizedPrimary == emirate.name) arabicNames[emirate.name] else emirate.name

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) palette.crimsonPillBg else palette.surfaceGradStart,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) palette.crimsonLink.copy(alpha = 0.55f) else palette.cardBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                locationManager.setEmirate(emirate.name)
                                onBack()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = localizedPrimary,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) palette.crimsonLink else palette.textPrimary
                                )
                                if (!secondaryName.isNullOrBlank()) {
                                    Text(
                                        text = secondaryName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textMuted
                                    )
                                }
                            }
                            if (isSelected) {
                                Surface(
                                    shape = CircleShape,
                                    color = palette.crimsonLink,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = stringResource(R.string.selected_label),
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = palette.textMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
