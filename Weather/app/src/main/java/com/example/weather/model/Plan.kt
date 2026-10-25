package com.example.weather.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class PlanType(val label: String) {
    Outdoor("Açık hava"),
    Indoor("Kapalı alan"),
    Travel("Yolculuk"),
}

enum class Reminder(val label: String, val minutesBefore: Long?) {
    None("Yok", null),
    OneHour("1 saat önce", 60),
    ThreeHours("3 saat önce", 180),
    OneDay("1 gün önce", 24 * 60),
}

enum class PlanSource { App, Calendar }

/**
 * A plan the user wants HavaCan to watch. [date] ("2026-10-25") and [time] ("17:00") are local
 * to the plan's [city], the same clock Open-Meteo's hourly forecast uses for that place.
 */
data class Plan(
    val id: String,
    val title: String,
    val date: String,
    val time: String,
    val city: City,
    val placeName: String? = null,
    val type: PlanType = PlanType.Outdoor,
    val follow: Boolean = true,
    val reminder: Reminder = Reminder.OneDay,
    val source: PlanSource = PlanSource.App,
    val calendarEventId: Long? = null,
    /** Last risk state we notified about, so the watcher only speaks up when it changes. */
    val lastNotified: String? = null,
) {
    val localDate: LocalDate get() = LocalDate.parse(date)
    val localTime: LocalTime get() = LocalTime.parse(time)
    val dateTime: LocalDateTime get() = LocalDateTime.of(localDate, localTime)

    /** Hourly forecast key of the hour the plan starts in, e.g. "2026-10-25T17:00". */
    val hourKey: String get() = "%sT%02d:00".format(date, localTime.hour)

    /** "17.00", the Turkish way of writing times. */
    val timeLabel: String get() = time.replace(':', '.')
}
