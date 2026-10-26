package com.kampplus.havacan.ui.navigation

import com.kampplus.havacan.ui.home.components.HomeTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HavaRoutesTest {
    @Test
    fun everyTabHasItsOwnRoute() {
        HomeTab.entries.forEach { assertEquals(it, tabOf(it.route)) }
        assertEquals(HomeTab.entries.size, HomeTab.entries.map { it.route }.distinct().size)
    }

    @Test
    fun fullScreenRoutesBelongToNoTab() {
        assertNull(tabOf(HavaRoutes.CITY))
        assertNull(tabOf(HavaRoutes.RUN))
        assertNull(tabOf(null))
    }

    @Test
    fun aNewRunHasNoArguments() {
        assertEquals("run", HavaRoutes.run())
    }
}
