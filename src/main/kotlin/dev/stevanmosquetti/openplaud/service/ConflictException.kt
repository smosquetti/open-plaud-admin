package dev.stevanmosquetti.openplaud.service

/** The operation would create a duplicate state. Mapped to HTTP 409. */
class ConflictException(message: String) : RuntimeException(message)
