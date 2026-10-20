package dev.stevanmosquetti.openplaud.client

/** Raised when the ejabberd mod_http_api reports a failure. */
class EjabberdException(message: String) : RuntimeException(message)
