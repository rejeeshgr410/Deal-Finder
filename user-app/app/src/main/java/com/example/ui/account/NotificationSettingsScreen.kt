package com.example.ui.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.auth.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notification Preferences", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (currentUser != null) {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        "Choose the updates and reminders you'd like to receive on your device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            PreferenceSwitch(
                                title = "New Flyers",
                                description = "Alerts when favorite stores publish new promotional leaflets",
                                checked = currentUser!!.notifyNewFlyers,
                                onCheckedChange = { authViewModel.updateUserPreferences(currentUser!!.copy(notifyNewFlyers = it)) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)
                            PreferenceSwitch(
                                title = "Price Drops",
                                description = "Notifications when items on your shopping lists decrease in price",
                                checked = currentUser!!.notifyPriceDrops,
                                onCheckedChange = { authViewModel.updateUserPreferences(currentUser!!.copy(notifyPriceDrops = it)) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)
                            PreferenceSwitch(
                                title = "Deal Expiry",
                                description = "Gentle reminders 24 hours before your saved deals expire",
                                checked = currentUser!!.notifyDealExpiry,
                                onCheckedChange = { authViewModel.updateUserPreferences(currentUser!!.copy(notifyDealExpiry = it)) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)
                            PreferenceSwitch(
                                title = "Upcoming Promotions",
                                description = "Seasonal sales and Ramadan / Eid festival discount alerts",
                                checked = currentUser!!.notifyPromotions,
                                onCheckedChange = { authViewModel.updateUserPreferences(currentUser!!.copy(notifyPromotions = it)) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)
                            PreferenceSwitch(
                                title = "Coupon Expiry",
                                description = "Reminders before collected vouchers and promo codes expire",
                                checked = currentUser!!.notifyCouponExpiry,
                                onCheckedChange = { authViewModel.updateUserPreferences(currentUser!!.copy(notifyCouponExpiry = it)) }
                            )
                        }
                    }
                }
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Notification Consent & Opt-Out",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Marketing and promotional alerts are optional. You can opt out per category below or disable all alerts at any time; preferences are enforced server-side before dispatch.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = {
                                    currentUser?.let { u ->
                                        authViewModel.updateUserPreferences(
                                            u.copy(
                                                notifyNewFlyers = false,
                                                notifyPriceDrops = false,
                                                notifyDealExpiry = false,
                                                notifyPromotions = false,
                                                notifyCouponExpiry = false
                                            )
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Unsubscribe from All Categories")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PreferenceSwitch(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
