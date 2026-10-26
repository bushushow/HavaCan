package com.kampplus.havacan.model

/** Someone coming to a Weather Meet, and where they'll be setting off from. */
data class MeetPerson(
    val name: String,
    val city: City
)

/** Who a meet-up plan is with; stored on the [Plan] so Planlarım can show it. */
data class MeetDetails(
    val people: List<MeetPerson>
)

/** The part of the day a meet-up may happen in; the hours are start times. */
enum class MeetWindow(val label: String, val hours: IntRange) {
    Morning("Sabah", 8..11),
    Afternoon("Öğleden sonra", 12..16),
    Evening("Akşam", 17..21)
}
