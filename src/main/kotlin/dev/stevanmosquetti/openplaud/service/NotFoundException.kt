package dev.stevanmosquetti.openplaud.service

/** An entity does not exist. Mapped to HTTP 404. */
class NotFoundException(message: String) : RuntimeException(message)
