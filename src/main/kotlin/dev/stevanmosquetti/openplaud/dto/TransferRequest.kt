package dev.stevanmosquetti.openplaud.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class TransferRequest(
    @NotBlank
    val room: String,

    @NotNull
    val operatorId: UUID,

    @NotNull
    val previousOperatorId: UUID,

    val service: String? = null,
)