package com.kampplus.havacan.ui.common

import com.google.gson.JsonParseException
import java.io.IOException
import retrofit2.HttpException

/**
 * The four states of anything loaded from the network, shared by every screen. Sealed, so a
 * `when` that forgets a branch does not compile.
 */
sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data class Success<T>(val value: T) : LoadState<T>
    data object Empty : LoadState<Nothing>
    data class Error(val reason: ErrorReason) : LoadState<Nothing>
}

/** Why loading failed, in terms the screen can explain to the user. */
enum class ErrorReason { NoConnection, Server, Unknown }

/**
 * Turns a repository exception into a reason, in the ViewModel: repositories throw as they are,
 * the ViewModel decides what the user hears.
 */
fun Throwable.toErrorReason(): ErrorReason = when (this) {
    is IOException -> ErrorReason.NoConnection
    is HttpException -> ErrorReason.Server
    is JsonParseException -> ErrorReason.Unknown
    else -> ErrorReason.Unknown
}
