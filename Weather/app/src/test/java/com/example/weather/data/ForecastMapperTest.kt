package com.example.weather.data

import com.example.weather.data.remote.ForecastResponse
import com.example.weather.model.WeatherCondition
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Uses a trimmed copy of a real Open-Meteo forecast response for Ankara. */
class ForecastMapperTest {
    private val gson = Gson()

    private val json = """
        {"timezone":"Europe/Istanbul","utc_offset_seconds":10800,
         "current":{"time":"2026-10-25T15:45","interval":900,"temperature_2m":22.0,"apparent_temperature":19.7,
                    "weather_code":0,"is_day":1,"wind_speed_10m":10.0,"precipitation":0.0},
         "hourly":{"time":["2026-10-25T14:00","2026-10-25T15:00","2026-10-25T16:00","2026-10-25T17:00"],
                   "temperature_2m":[21.5,22.0,null,19.0],
                   "apparent_temperature":[19.0,19.7,null,17.1],
                   "precipitation_probability":[0,5,null,70],
                   "precipitation":[0.0,0.0,null,1.2],
                   "weather_code":[0,1,2,61],
                   "wind_speed_10m":[8.0,10.0,null,14.0],
                   "wind_gusts_10m":[15.0,18.0,null,30.0],
                   "uv_index":[3.0,2.0,null,0.5],
                   "is_day":[1,1,1,1]},
         "daily":{"time":["2026-10-25","2026-10-26"],"temperature_2m_max":[22.2,21.3],"temperature_2m_min":[7.7,6.4],
                  "sunrise":["2026-10-25T07:09","2026-10-26T07:10"],"sunset":["2026-10-25T17:55","2026-10-26T17:53"]}}
    """.trimIndent()

    private fun parse(text: String) = ForecastMapper.map(gson.fromJson(text, ForecastResponse::class.java))

    @Test
    fun mapsCurrentHourlyAndToday() {
        val forecast = parse(json)!!
        assertEquals("Europe/Istanbul", forecast.timezone)
        assertEquals(22.0, forecast.current.temperature, 0.0)
        assertEquals(WeatherCondition.Clear, forecast.current.condition)
        assertEquals(22.2, forecast.today!!.temperatureMax, 0.0)
        assertEquals("2026-10-25T17:55", forecast.today!!.sunset)
    }

    @Test
    fun dropsHoursWithoutTemperatureAndFindsCurrentHour() {
        val forecast = parse(json)!!
        assertEquals(listOf(14, 15, 17), forecast.hourly.map { it.hour })
        assertEquals(1, forecast.currentHourIndex)
        assertEquals(listOf("15.00", "17.00"), forecast.upcoming.map { it.hourLabel })
        assertEquals(WeatherCondition.Rain, forecast.hourly.last().condition)
        assertTrue(forecast.hourly.last().condition.isWet)
    }

    @Test
    fun missingCurrentOrHourlyMeansNoForecast() {
        assertNull(parse("""{"timezone":"Europe/Istanbul"}"""))
        assertNull(parse("""{"current":{"time":"2026-10-25T15:45","temperature_2m":22.0},"hourly":{"time":[]}}"""))
    }

    @Test
    fun wmoCodesMapToConditions() {
        assertEquals(WeatherCondition.PartlyCloudy, WeatherCondition.fromWmo(2))
        assertEquals(WeatherCondition.HeavyRain, WeatherCondition.fromWmo(82))
        assertEquals(WeatherCondition.Snow, WeatherCondition.fromWmo(73))
        assertEquals(WeatherCondition.Thunderstorm, WeatherCondition.fromWmo(95))
        assertEquals(WeatherCondition.Unknown, WeatherCondition.fromWmo(null))
    }
}
