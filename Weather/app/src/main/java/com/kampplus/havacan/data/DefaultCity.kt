package com.kampplus.havacan.data

import com.kampplus.havacan.model.City

/** Where HavaCan starts before the user picks a place: Ankara. */
val DEFAULT_CITY: City = provinceCities.first { it.name == "Ankara" }
