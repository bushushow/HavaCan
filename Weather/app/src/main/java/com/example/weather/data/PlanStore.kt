package com.example.weather.data

import com.example.weather.model.Plan
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

interface PlanRepository {
    val plans: StateFlow<List<Plan>>
    suspend fun load()
    suspend fun upsert(plan: Plan)
    suspend fun upsertAll(plans: List<Plan>)
    suspend fun delete(id: String)
    suspend fun get(id: String): Plan?
}

/**
 * Plans live in a small JSON file in the app's private storage. Writes go to a temp file first
 * and are then renamed, so a crash mid-write can't leave a half-written file.
 */
class FilePlanStore(
    private val file: File,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : PlanRepository {
    private val gson = Gson()
    private val mutex = Mutex()
    private val state = MutableStateFlow<List<Plan>>(emptyList())
    private var loaded = false

    override val plans: StateFlow<List<Plan>> = state.asStateFlow()

    override suspend fun load() {
        mutex.withLock { ensureLoaded() }
    }

    override suspend fun get(id: String): Plan? = mutex.withLock {
        ensureLoaded()
        state.value.firstOrNull { it.id == id }
    }

    override suspend fun upsert(plan: Plan) = upsertAll(listOf(plan))

    override suspend fun upsertAll(plans: List<Plan>) = mutex.withLock {
        ensureLoaded()
        val ids = plans.map { it.id }.toSet()
        write(state.value.filterNot { it.id in ids } + plans)
    }

    override suspend fun delete(id: String) = mutex.withLock {
        ensureLoaded()
        write(state.value.filterNot { it.id == id })
    }

    private suspend fun ensureLoaded() {
        if (loaded) return
        state.value = withContext(io) {
            if (!file.exists()) {
                emptyList()
            } else {
                runCatching {
                    gson.fromJson<List<Plan>>(file.readText(), object : TypeToken<List<Plan>>() {}.type)
                }.getOrNull().orEmpty()
            }
        }
        loaded = true
    }

    private suspend fun write(plans: List<Plan>) {
        val sorted = plans.sortedBy { it.dateTime }
        withContext(io) {
            file.parentFile?.mkdirs()
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(gson.toJson(sorted))
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
        }
        state.value = sorted
    }
}
