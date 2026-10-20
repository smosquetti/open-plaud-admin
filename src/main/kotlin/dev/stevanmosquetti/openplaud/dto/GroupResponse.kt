package dev.stevanmosquetti.openplaud.dto

import java.util.UUID

data class GroupResponse(
    val id: UUID,
    val name: String,
    /** MUC room name derived from [name], e.g. `support` for `Support Team`. */
    val room: String,
    val service: String,
)
