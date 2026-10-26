package dev.stevanmosquetti.openplaud.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

/**
 * Request for an operator to join a temp MUC room.
 */
class JoinChatroomRequest (
    @NotNull
    val operatorId: UUID,

    @NotNull
    val groupId: UUID,

    /**
     * Name (node) of the temp MUC room.
     */
    @NotBlank
    val room: String,
    
    val service: String? = null,
)