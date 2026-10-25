package com.example.weather

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.weather.ui.HavaApp
import com.example.weather.ui.theme.WeatherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Light status bar icons in both themes: they sit on the blue header gradient
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        setContent {
            WeatherTheme {
                HavaApp()
            }
        }
    }
}
