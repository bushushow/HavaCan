package com.kampplus.havacan.data

import com.kampplus.havacan.model.Plan

/** Reminders and the weather watch for plans; implemented with WorkManager in the app. */
interface PlanScheduler {
    fun schedule(plan: Plan)
    fun cancel(planId: String)
}
