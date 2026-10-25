package com.example.weather

import android.graphics.Color
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import java.util.Locale
import android.os.Bundle
import androidx.compose.runtime.mutableStateOf
import com.example.weather.ui.plans.EXTRA_PLAN_ID
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.weather.ui.HavaApp
import com.example.weather.ui.theme.WeatherTheme

class MainActivity : ComponentActivity() {
    /** Plan to open when the app was launched from a plan notification. */
    private val openPlanId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openPlanId.value = intent.getStringExtra(EXTRA_PLAN_ID)
        // Dark status bar icons on the ivory background (light icons in dark theme)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT))
        setContent {
            WeatherTheme {
                HavaApp(
                    openPlanId = openPlanId.value,
                    onOpenPlanHandled = { openPlanId.value = null },
                )
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
