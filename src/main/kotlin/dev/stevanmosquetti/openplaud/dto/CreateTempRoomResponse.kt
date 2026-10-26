package dev.stevanmosquetti.openplaud.dto

/**
 * Response with the created temp room name and MUC service.
 */
data class CreateTempRoomResponse(
    val room: String,
    val service: String,
)
