package dev.stevanmosquetti.openplaud.service

/** A display name cannot be turned into a valid XMPP localpart. Mapped to HTTP 422. */
class InvalidSlugException(message: String) : RuntimeException(message)
