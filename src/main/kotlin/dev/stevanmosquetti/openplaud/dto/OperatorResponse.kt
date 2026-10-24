package dev.stevanmosquetti.openplaud.dto

import java.util.UUID
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class OperatorResponse(
    val id: UUID,
    val name: String,
    val email: String,
    /** XMPP localpart derived from [name], e.g. `johndoe` for `John Doe`. */
    val xmppUser: String,
    /** The XMPP login password. Returned only on creation. */
    val initialPassword: String? = null,
)
