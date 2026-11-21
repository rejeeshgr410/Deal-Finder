package com.example.ui.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateUtils
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
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ui.DealFinderViewModel
import com.example.ui.components.HomeTopBackgroundWash
import com.example.ui.theme.CatPurple
import com.example.ui.theme.CatPurpleIcon
import com.example.ui.theme.FigmaCrimsonEnd
import com.example.ui.theme.FigmaCrimsonStart
import com.example.ui.theme.LocalAppPalette

@Composable
fun NotificationsScreen(
    viewModel: DealFinderViewModel,
    isSignedIn: Boolean,
    onNavigateToAuth: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onBack: () -> Unit
) {
    val palette = LocalAppPalette.current
    val context = LocalContext.current
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount by viewModel.unreadNotificationCount.collectAsState()

    var hasPushPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPushPermission = granted
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
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
                        text = stringResource(R.string.notifications_title),
                        color = palette.textPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (unreadCount > 0) {
                        Text(
                            text = stringResource(R.string.mark_all_read),
                            color = palette.crimsonLink,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.markAllNotificationsRead() }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = palette.surfaceGradStart,
                        border = BorderStroke(1.dp, palette.cardBorder),
                        modifier = Modifier
                            .size(40.dp)
                            .shadow(4.dp, CircleShape, clip = false)
                            .clip(CircleShape)
                            .clickable { onNavigateToSettings() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = stringResource(R.string.notification_preferences),
                                tint = palette.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Runtime POST_NOTIFICATIONS banner for Android 13+
            if (!hasPushPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.crimsonPillBg,
                    border = BorderStroke(1.dp, palette.crimsonPillBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = palette.surfaceGradStart,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.NotificationsActive,
                                    contentDescription = null,
                                    tint = palette.crimsonLink,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.enable_push_permission),
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = stringResource(R.string.push_permission_desc),
                                color = palette.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = palette.crimsonLink,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                        ) {
                            Text(
                                text = stringResource(R.string.enable_notifications).substringBefore(" "),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            if (notifications.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (palette.isDark) CatPurpleIcon.copy(alpha = 0.2f) else CatPurple,
                        modifier = Modifier.size(84.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = CatPurpleIcon
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.no_notifications),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isSignedIn) {
                            stringResource(R.string.notifications_empty_desc)
                        } else {
                            stringResource(R.string.notifications_guest_desc)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    if (!isSignedIn) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Brush.horizontalGradient(listOf(FigmaCrimsonStart, FigmaCrimsonEnd)))
                                .clickable { onNavigateToAuth() }
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.sign_in),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Brush.horizontalGradient(listOf(FigmaCrimsonStart, FigmaCrimsonEnd)))
                                .clickable { onNavigateToSettings() }
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.notification_preferences),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notifications, key = { it.id }) { notification ->
                        val relativeTime = remember(notification.timestamp) {
                            if (notification.timestamp > 0L) {
                                DateUtils.getRelativeTimeSpanString(
                                    notification.timestamp,
                                    System.currentTimeMillis(),
                                    DateUtils.MINUTE_IN_MILLIS
                                ).toString()
                            } else ""
                        }
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = palette.surfaceGradStart,
                            border = BorderStroke(
                                1.dp,
                                if (!notification.isRead) palette.crimsonLink.copy(alpha = 0.45f) else palette.cardBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(if (!notification.isRead) 4.dp else 1.dp, RoundedCornerShape(18.dp), clip = false)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { viewModel.markNotificationRead(notification.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (!notification.isRead) palette.crimsonPillBg else palette.subtleSurface,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (!notification.isRead) Icons.Outlined.NotificationsActive else Icons.Outlined.Notifications,
                                            contentDescription = null,
                                            tint = if (!notification.isRead) palette.crimsonLink else palette.textMuted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = notification.title,
                                            color = palette.textPrimary,
                                            fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (!notification.isRead) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(palette.crimsonLink)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = notification.message,
                                        color = palette.textSecondary,
                                        fontSize = 12.5.sp,
                                        lineHeight = 18.sp
                                    )
                                    if (relativeTime.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = relativeTime,
                                            color = palette.textMuted,
                                            fontSize = 11.sp
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
}
