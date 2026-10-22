package dev.stevanmosquetti.openplaud.dto

import jakarta.validation.constraints.NotBlank

data class CreateOperatorRequest(
    @NotBlank
    val name: String,
    
    @NotBlank
    val email: String,

    /**
     * XMPP login password for the new user. Optional — when omitted, the server
     * generates one and returns it (once) in the response.
     */
    val password: String? = null,
)
