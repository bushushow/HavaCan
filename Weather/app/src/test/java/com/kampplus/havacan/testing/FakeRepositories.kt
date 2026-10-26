package com.kampplus.havacan.testing

import com.kampplus.havacan.data.LocationStore
import com.kampplus.havacan.data.PlanScheduler
import com.kampplus.havacan.model.City
import com.kampplus.havacan.model.Plan

/** A remembered city kept in memory. */
class FakeLocationStore(var city: City?) : LocationStore {
    override fun load(): City? = city

    override fun save(city: City) {
        this.city = city
    }
}

/** Records which plans have reminders scheduled, instead of talking to WorkManager. */
class RecordingScheduler : PlanScheduler {
    val scheduled = mutableListOf<String>()

    override fun schedule(plan: Plan) {
        scheduled += plan.id
    }

    override fun cancel(planId: String) {
        scheduled -= planId
    }
}
