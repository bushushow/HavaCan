package com.kampplus.havacan

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.kampplus.havacan.di.AppContainer
import com.kampplus.havacan.ui.HavaApp
import com.kampplus.havacan.ui.plans.EXTRA_PLAN_ID
import com.kampplus.havacan.ui.theme.DisplaySettings
import com.kampplus.havacan.ui.theme.LocalDisplaySettings
import com.kampplus.havacan.ui.theme.WeatherTheme
import java.util.Locale
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    /** Plan to open when the app was launched from a plan notification. */
    private val openPlanId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch { AppContainer.get(applicationContext).workScheduler.migrateWorkOnce() }
        openPlanId.value = intent.getStringExtra(EXTRA_PLAN_ID)
        // Dark status bar icons on the ivory background (light icons in dark theme)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT))
        setContent {
            val prefs by AppContainer.get(this).profile.prefs.collectAsStateWithLifecycle()
            val settings = DisplaySettings(prefs.unit, prefs.largeText, prefs.highContrast, prefs.reduceMotion)
            WeatherTheme {
                // Profil > Erişilebilirlik > Büyük yazı: 20 % on top of Android's own font size
                val density = LocalDensity.current
                val scaled = if (settings.largeText) Density(density.density, density.fontScale * 1.2f) else density
                CompositionLocalProvider(LocalDensity provides scaled, LocalDisplaySettings provides settings) {
                    HavaApp(
                        openPlanId = openPlanId.value,
                        onOpenPlanHandled = { openPlanId.value = null }
                    )
                }
            }
        }
    }

    /** HavaCan is a Turkish app: date and time pickers follow Turkish even on an English phone. */
    override fun attachBaseContext(newBase: Context) {
        val config = Configuration(newBase.resources.configuration).apply { setLocale(Locale.forLanguageTag("tr")) }
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_PLAN_ID)?.let { openPlanId.value = it }
    }
}
