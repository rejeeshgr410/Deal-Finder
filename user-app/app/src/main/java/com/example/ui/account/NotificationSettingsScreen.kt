package com.example.ui.account

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
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
import com.example.ui.auth.AuthViewModel
import com.example.ui.components.HomeTopBackgroundWash
import com.example.ui.theme.FigmaCrimsonEnd
import com.example.ui.theme.FigmaCrimsonStart
import com.example.ui.theme.LocalAppPalette

@Composable
fun NotificationSettingsScreen(
    authViewModel: AuthViewModel,
    onNavigateToAuth: () -> Unit,
    onBack: () -> Unit
) {
    val palette = LocalAppPalette.current
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val currentUserId by authViewModel.currentUserId.collectAsState()
    val preferenceError by authViewModel.preferenceError.collectAsState()

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
                    text = stringResource(R.string.notification_preferences),
                    color = palette.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (currentUserId == null || currentUser == null) {
                // Signed-out empty state (no longer blank)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = palette.crimsonPillBg,
                        modifier = Modifier.size(84.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsOff,
                                contentDescription = null,
                                tint = palette.crimsonLink,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.notification_preferences),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.notifications_guest_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
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
                }
            } else {
                val user = currentUser!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (preferenceError != null) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = preferenceError!!,
                                        color = if (palette.isDark) palette.textPrimary else MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = stringResource(R.string.close),
                                        color = palette.crimsonLink,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.clickable { authViewModel.clearPreferenceError() }
                                    )
                                }
                            }
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (hasPushPermission) palette.surfaceGradStart else palette.crimsonPillBg,
                                border = BorderStroke(
                                    1.dp,
                                    if (hasPushPermission) palette.cardBorder else palette.crimsonPillBorder
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.NotificationsActive,
                                        contentDescription = null,
                                        tint = palette.crimsonLink,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (hasPushPermission) {
                                                stringResource(R.string.push_permission_granted)
                                            } else {
                                                stringResource(R.string.enable_push_permission)
                                            },
                                            color = palette.textPrimary,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        if (!hasPushPermission) {
                                            Text(
                                                text = stringResource(R.string.push_permission_desc),
                                                color = palette.textSecondary,
                                                fontSize = 11.5.sp
                                            )
                                        }
                                    }
                                    if (!hasPushPermission) {
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
                                                text = stringResource(R.string.add_btn),
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = stringResource(R.string.notification_preferences_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = palette.surfaceGradStart,
                            border = BorderStroke(1.dp, palette.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                PreferenceSwitch(
                                    title = stringResource(R.string.notif_new_flyers),
                                    description = stringResource(R.string.notif_new_flyers_desc),
                                    checked = user.notifyNewFlyers,
                                    onCheckedChange = {
                                        authViewModel.updateUserPreferences(user.copy(notifyNewFlyers = it))
                                    }
                                )
                                HorizontalDivider(color = palette.divider, thickness = 0.6.dp)
                                PreferenceSwitch(
                                    title = stringResource(R.string.notif_price_drops),
                                    description = stringResource(R.string.notif_price_drops_desc),
                                    checked = user.notifyPriceDrops,
                                    onCheckedChange = {
                                        authViewModel.updateUserPreferences(user.copy(notifyPriceDrops = it))
                                    }
                                )
                                HorizontalDivider(color = palette.divider, thickness = 0.6.dp)
                                PreferenceSwitch(
                                    title = stringResource(R.string.notif_deal_expiry),
                                    description = stringResource(R.string.notif_deal_expiry_desc),
                                    checked = user.notifyDealExpiry,
                                    onCheckedChange = {
                                        authViewModel.updateUserPreferences(user.copy(notifyDealExpiry = it))
                                    }
                                )
                                HorizontalDivider(color = palette.divider, thickness = 0.6.dp)
                                PreferenceSwitch(
                                    title = stringResource(R.string.notif_upcoming_promos),
                                    description = stringResource(R.string.notif_upcoming_promos_desc),
                                    checked = user.notifyPromotions,
                                    onCheckedChange = {
                                        authViewModel.updateUserPreferences(user.copy(notifyPromotions = it))
                                    }
                                )
                                HorizontalDivider(color = palette.divider, thickness = 0.6.dp)
                                PreferenceSwitch(
                                    title = stringResource(R.string.notif_coupon_expiry),
                                    description = stringResource(R.string.notif_coupon_expiry_desc),
                                    checked = user.notifyCouponExpiry,
                                    onCheckedChange = {
                                        authViewModel.updateUserPreferences(user.copy(notifyCouponExpiry = it))
                                    }
                                )
                            }
                        }
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = palette.subtleSurface,
                            border = BorderStroke(1.dp, palette.cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.notif_consent_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = stringResource(R.string.notif_consent_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                                OutlinedButton(
                                    onClick = {
                                        authViewModel.updateUserPreferences(
                                            user.copy(
                                                notifyNewFlyers = false,
                                                notifyPriceDrops = false,
                                                notifyDealExpiry = false,
                                                notifyPromotions = false,
                                                notifyCouponExpiry = false
                                            )
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, palette.crimsonLink.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = stringResource(R.string.notif_unsubscribe_all),
                                        color = palette.crimsonLink,
                                        fontWeight = FontWeight.SemiBold
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

@Composable
fun PreferenceSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalAppPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textMuted
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = palette.crimsonLink
            )
        )
    }
}
