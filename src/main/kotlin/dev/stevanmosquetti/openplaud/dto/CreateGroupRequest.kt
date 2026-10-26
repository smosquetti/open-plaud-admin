package dev.stevanmosquetti.openplaud.dto

import jakarta.validation.constraints.NotBlank

/**
 * Request to create a group.
 */
data class CreateGroupRequest(
    @NotBlank
    val name: String,

    /**
     * MUC service the room is created on.
     */
    @NotBlank
    val service: String,
)
