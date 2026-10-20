package dev.stevanmosquetti.openplaud.dto

data class ApiError(
    val status: Int,
    val error: String,
    val message: String,
    val fields: Map<String, String> = emptyMap(),
)
