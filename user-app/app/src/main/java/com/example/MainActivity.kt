package com.example

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Resources
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.DealFinderApp
import com.example.ui.Screen
import com.example.ui.components.HomeTopBackgroundWash
import com.example.ui.theme.LocalThemeManager
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

  override fun attachBaseContext(newBase: Context) {
    val savedLang = LocaleManager.getSavedLanguage(newBase)
    super.attachBaseContext(LocaleManager.applyLocale(newBase, savedLang))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    CrashReporter.initialize()
    AnalyticsManager.initialize()
    BackupRecoveryManager(applicationContext)
    DemoRegistry.init(applicationContext)

    FirebaseManager.init(applicationContext)
    val db = FirebaseManager.getDb()

    val roomDb = androidx.room.Room.databaseBuilder(
        applicationContext,
        DealFinderRoomDatabase::class.java,
        "dealfinder-room-db"
    ).fallbackToDestructiveMigration(true).build()

    val sessionManager = SessionManager(applicationContext)
    val shoppingRepo = ShoppingListRepository(roomDb.shoppingListDao(), db, sessionManager)
    val dealFinderDao = FirestoreDealFinderDao(db, sessionManager)
    val dealFinderRepo = DealFinderRepository(dealFinderDao, roomDb.cachedFlyerDao())
    val loyaltyCardDao = roomDb.loyaltyCardDao()
    val locationManager = LocationManager(applicationContext)
    val authRepo = AuthRepository(dealFinderDao, sessionManager)
    val themeManager = ThemeManager(applicationContext)
    val localeManager = LocaleManager(applicationContext)

    // Enforce rememberMe == false on fresh process start if user did not ask to stay signed in
    authRepo.enforceSessionPersistenceOnLaunch(applicationContext)

    val initialRoute = intent?.getStringExtra("route") ?: Screen.Home.route

    setContent {
      val themeMode by themeManager.themeMode.collectAsState()
      val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.AUTO -> androidx.compose.foundation.isSystemInDarkTheme()
      }

      val currentLanguage by localeManager.currentLanguage.collectAsState()
      val layoutDirection = if (currentLanguage == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
      val localizedContext = remember(currentLanguage) {
        val cfgCtx = LocaleManager.applyLocale(this@MainActivity, currentLanguage)
        object : ContextWrapper(this@MainActivity) {
          override fun getResources(): Resources = cfgCtx.resources
          override fun getAssets(): AssetManager = cfgCtx.assets
        }
      }

      var showSplash by rememberSaveable { mutableStateOf(true) }
      LaunchedEffect(Unit) {
        if (showSplash) {
          delay(1450L)
          showSplash = false
        }
      }

      CompositionLocalProvider(
          LocalThemeManager provides themeManager,
          LocalLocaleManager provides localeManager,
          LocalLayoutDirection provides layoutDirection,
          LocalContext provides localizedContext,
          LocalActivityResultRegistryOwner provides this@MainActivity,
          LocalOnBackPressedDispatcherOwner provides this@MainActivity
      ) {
        MyApplicationTheme(darkTheme = darkTheme) {
          Box(modifier = Modifier.fillMaxSize()) {
            DealFinderApp(
                shoppingRepo = shoppingRepo,
                dealFinderRepo = dealFinderRepo,
                authRepo = authRepo,
                locationManager = locationManager,
                loyaltyCardDao = loyaltyCardDao,
                localeManager = localeManager,
                initialRoute = initialRoute
            )

            AnimatedVisibility(
                visible = showSplash,
                exit = fadeOut(animationSpec = tween(durationMillis = 400))
            ) {
              DealFinderSplashOverlay()
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DealFinderSplashOverlay() {
  Box(
      modifier = Modifier
          .fillMaxSize()
          .background(
              Brush.verticalGradient(
                  listOf(
                      Color(0xFFFFFDFE),
                      Color(0xFFFFFAFB),
                      Color(0xFFFFF5F7)
                  )
              )
          )
  ) {
    HomeTopBackgroundWash(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .align(Alignment.TopCenter)
    )

    Image(
        painter = painterResource(id = R.drawable.splash_dubai_skyline),
        contentDescription = null,
        contentScale = ContentScale.FillWidth,
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
      Image(
          painter = painterResource(id = R.drawable.deal_finder_logo),
          contentDescription = stringResource(R.string.app_name),
          contentScale = ContentScale.Fit,
          modifier = Modifier
              .size(136.dp)
              .shadow(
                  elevation = 18.dp,
                  shape = RoundedCornerShape(32.dp),
                  ambientColor = Color(0x45E90029),
                  spotColor = Color(0x45E90029)
              )
      )

      Spacer(modifier = Modifier.height(22.dp))

      Text(
          text = buildAnnotatedString {
            withStyle(SpanStyle(color = Color(0xFF0B1228))) { append("Deal") }
            withStyle(SpanStyle(color = Color(0xFFE90029))) { append("Finder") }
          },
          fontSize = 38.sp,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = (-0.8).sp
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
          text = stringResource(R.string.splash_tagline),
          fontSize = 15.sp,
          fontWeight = FontWeight.Medium,
          color = Color(0xFF3A3E52)
      )

      Spacer(modifier = Modifier.height(30.dp))

      CircularProgressIndicator(
          modifier = Modifier.size(36.dp),
          color = Color(0xFFD90023),
          trackColor = Color(0xFFFFD8E1),
          strokeWidth = 3.5.dp
      )
    }
  }
}
