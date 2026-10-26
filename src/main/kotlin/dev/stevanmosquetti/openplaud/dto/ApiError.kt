package dev.stevanmosquetti.openplaud.dto

/**
 * Standard JSON error body returned by the global exception handler.
 */
data class ApiError(
    val status: Int,
    val error: String,
    val message: String,
    val fields: Map<String, String> = emptyMap(),
)
