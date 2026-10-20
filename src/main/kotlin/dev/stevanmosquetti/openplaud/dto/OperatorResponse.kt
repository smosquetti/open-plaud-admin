package dev.stevanmosquetti.openplaud.dto

import java.util.UUID

data class OperatorResponse(
    val id: UUID,
    val name: String,
    /** XMPP localpart derived from [name], e.g. `johndoe` for `John Doe`. */
    val xmppUser: String,
    /** The XMPP login password. Returned only on creation. */
    val initialPassword: String? = null,
)
