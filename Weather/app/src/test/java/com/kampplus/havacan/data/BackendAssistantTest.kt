package com.kampplus.havacan.data

import com.kampplus.havacan.domain.voice.AiAction
import org.junit.Assert.assertEquals
import org.junit.Test

class BackendAssistantTest {
    @Test
    fun backendJsonIsParsed() {
        val d = BackendAssistant.parse(
            """{"reply":null,"action":{"name":"update_plan","args":{"plan_id":"p1","time":"19:00","indoor":true}}}"""
        )
        assertEquals(AiAction("update_plan", planId = "p1", time = "19:00", indoor = true), d.action)
    }
}
