package dev.stevanmosquetti.openplaud.client

import org.springframework.http.HttpStatusCode

/** Raised when the ejabberd mod_http_api reports a failure. */
class EjabberdException(message: String, val statusCode: HttpStatusCode, val status: String) : RuntimeException(message) {
}
