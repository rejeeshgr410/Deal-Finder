package com.example.ui.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                title = { Text("Notification Preferences") },
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
                modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text("Select which notifications you want to receive.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                item {
                    PreferenceSwitch(
                        title = "New Flyers",
                        description = "Get notified when your favorite stores release new flyers",
                        checked = currentUser!!.notifyNewFlyers,
                        onCheckedChange = { authViewModel.updateUserPreferences(currentUser!!.copy(notifyNewFlyers = it)) }
                    )
                }
                item {
                    PreferenceSwitch(
                        title = "Price Drops",
                        description = "Alerts for price drops on items in your shopping list",
                        checked = currentUser!!.notifyPriceDrops,
                        onCheckedChange = { authViewModel.updateUserPreferences(currentUser!!.copy(notifyPriceDrops = it)) }
                    )
                }
                item {
                    PreferenceSwitch(
                        title = "Deal Expiry",
                        description = "Reminders before flyer deals expire",
                        checked = currentUser!!.notifyDealExpiry,
                        onCheckedChange = { authViewModel.updateUserPreferences(currentUser!!.copy(notifyDealExpiry = it)) }
                    )
                }
                item {
                    PreferenceSwitch(
                        title = "Upcoming Promotions",
                        description = "Updates about upcoming sales and promotions",
                        checked = currentUser!!.notifyPromotions,
                        onCheckedChange = { authViewModel.updateUserPreferences(currentUser!!.copy(notifyPromotions = it)) }
                    )
                }
                item {
                    PreferenceSwitch(
                        title = "Coupon Expiry",
                        description = "Reminders before your collected coupons expire",
                        checked = currentUser!!.notifyCouponExpiry,
                        onCheckedChange = { authViewModel.updateUserPreferences(currentUser!!.copy(notifyCouponExpiry = it)) }
                    )
                }
            }
        }
    }
}

@Composable
fun PreferenceSwitch(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
