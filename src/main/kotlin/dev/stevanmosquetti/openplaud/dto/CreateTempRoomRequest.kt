package dev.stevanmosquetti.openplaud.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class CreateTempRoomRequest(
    @NotBlank
    val user: String,
    @NotNull
    val groupId: UUID,
    val service: String? = null,
)
