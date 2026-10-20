package dev.stevanmosquetti.openplaud.dto

import jakarta.validation.constraints.NotBlank

data class CreateGroupRequest(
    @NotBlank
    val name: String,

    @NotBlank
    val service: String,
)
