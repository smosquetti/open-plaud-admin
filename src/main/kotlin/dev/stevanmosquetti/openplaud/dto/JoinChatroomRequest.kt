package dev.stevanmosquetti.openplaud.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.util.UUID

class JoinChatroomRequest (
    @NotNull
    val operatorId: UUID,
    
    @NotNull
    val groupId: UUID,
    
    @NotBlank
    val room: String,
    
    val service: String? = null,
)