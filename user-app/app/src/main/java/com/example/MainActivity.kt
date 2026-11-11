package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import com.example.data.FirebaseManager
import com.example.data.FirestoreShoppingListDao
import com.example.data.FirestoreDealFinderDao
import com.example.data.ShoppingListRepository
import com.example.ui.DealFinderApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    com.example.data.CrashReporter.initialize()
    com.example.data.AnalyticsManager.initialize()
    val backupRecoveryManager = com.example.data.BackupRecoveryManager(applicationContext)

    FirebaseManager.init(applicationContext)
    val db = FirebaseManager.getDb()
    
    val roomDb = androidx.room.Room.databaseBuilder(
        applicationContext,
        com.example.data.DealFinderRoomDatabase::class.java, "dealfinder-room-db"
    ).build()
    
    val sessionManager = com.example.data.SessionManager(applicationContext)
    val shoppingRepo = ShoppingListRepository(roomDb.shoppingListDao(), db, sessionManager)
    val dealFinderDao = FirestoreDealFinderDao(db, sessionManager)
    val dealFinderRepo = com.example.data.DealFinderRepository(dealFinderDao, roomDb.cachedFlyerDao())
    val locationManager = com.example.data.LocationManager(applicationContext)
    val authRepo = com.example.data.AuthRepository(dealFinderDao, sessionManager)
    val themeManager = com.example.data.ThemeManager(applicationContext)
    
    val initialRoute = intent?.getStringExtra("route") ?: com.example.ui.Screen.Home.route

    setContent {
      val themeMode by themeManager.themeMode.collectAsState()
      val darkTheme = when (themeMode) {
          com.example.data.ThemeMode.LIGHT -> false
          com.example.data.ThemeMode.DARK -> true
          com.example.data.ThemeMode.AUTO -> androidx.compose.foundation.isSystemInDarkTheme()
      }

      androidx.compose.runtime.CompositionLocalProvider(
          com.example.ui.theme.LocalThemeManager provides themeManager
      ) {
          MyApplicationTheme(darkTheme = darkTheme) {
            DealFinderApp(shoppingRepo, dealFinderRepo, authRepo, locationManager, initialRoute)
          }
      }
    }
  }
}



