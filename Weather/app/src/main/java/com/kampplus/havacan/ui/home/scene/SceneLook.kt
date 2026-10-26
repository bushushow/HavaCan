package com.kampplus.havacan.ui.home.scene

import com.kampplus.havacan.model.WeatherCondition

/**
 * How the sky should look, as 0..1 intensities. Each value is animated separately, so moving
 * from a sunny hour to a rainy one dims the sun, greys the clouds and starts the rain gradually.
 */
data class SceneLook(
    val sun: Float = 1f,
    val night: Float = 0f,
    val clouds: Float = 0.4f,
    val overcast: Float = 0f,
    val rain: Float = 0f,
    val snow: Float = 0f
) {
    companion object {
        fun of(condition: WeatherCondition?, isDay: Boolean): SceneLook {
            val base = when (condition) {
                WeatherCondition.Clear, null, WeatherCondition.Unknown -> SceneLook(sun = 1f, clouds = 0.25f)
                WeatherCondition.MainlyClear -> SceneLook(sun = 1f, clouds = 0.55f)
                WeatherCondition.PartlyCloudy -> SceneLook(sun = 0.9f, clouds = 0.85f)
                WeatherCondition.Overcast -> SceneLook(sun = 0.25f, clouds = 1f, overcast = 0.6f)
                WeatherCondition.Fog -> SceneLook(sun = 0.2f, clouds = 1f, overcast = 0.8f)
                WeatherCondition.Drizzle -> SceneLook(sun = 0.15f, clouds = 1f, overcast = 0.7f, rain = 0.35f)
                WeatherCondition.Rain -> SceneLook(sun = 0f, clouds = 1f, overcast = 0.85f, rain = 0.7f)
                WeatherCondition.HeavyRain, WeatherCondition.Thunderstorm ->
                    SceneLook(sun = 0f, clouds = 1f, overcast = 1f, rain = 1f)
                WeatherCondition.Snow -> SceneLook(sun = 0.1f, clouds = 1f, overcast = 0.5f, snow = 0.8f)
            }
            return if (isDay) base else base.copy(night = 1f)
        }
    }
}
