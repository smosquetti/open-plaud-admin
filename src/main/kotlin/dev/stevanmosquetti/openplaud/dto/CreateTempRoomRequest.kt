package dev.stevanmosquetti.openplaud.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

/**
 * Request to create a temp MUC room.
 */
data class CreateTempRoomRequest(
    /**
     * XMPP username (localpart) the room is created for.
     */
    @NotBlank
    val user: String,
    
    @NotNull
    val groupId: UUID,
    
    val service: String? = null,
)
