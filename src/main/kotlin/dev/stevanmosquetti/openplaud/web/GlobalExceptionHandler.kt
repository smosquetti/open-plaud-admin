package dev.stevanmosquetti.openplaud.web

import dev.stevanmosquetti.openplaud.client.EjabberdException
import dev.stevanmosquetti.openplaud.dto.ApiError
import dev.stevanmosquetti.openplaud.service.ConflictException
import dev.stevanmosquetti.openplaud.service.InvalidSlugException
import dev.stevanmosquetti.openplaud.service.NotFoundException
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Translates service-layer and transport failures into consistent JSON error bodies.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /**
     * Maps NotFoundException to 404 Not Found.
     */
    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(e: NotFoundException): ResponseEntity<ApiError> =
        respond(HttpStatus.NOT_FOUND, "not_found", e.message ?: "Resource not found")

    /**
     * Maps ConflictException to 409 Conflict.
     */
    @ExceptionHandler(ConflictException::class)
    fun handleConflict(e: ConflictException): ResponseEntity<ApiError> =
        respond(HttpStatus.CONFLICT, "conflict", e.message ?: "Conflict")

    /**
     * Maps InvalidSlugException to 422 Unprocessable Entity.
     */
    @ExceptionHandler(InvalidSlugException::class)
    fun handleInvalidSlug(e: InvalidSlugException): ResponseEntity<ApiError> =
        respond(HttpStatus.UNPROCESSABLE_ENTITY, "invalid_slug", e.message ?: "Invalid display name")

    /**
     * Proxies an upstream ejabberd failure with its original status code.
     */
    @ExceptionHandler(EjabberdException::class)
    fun handleEjabberd(e: EjabberdException, request: HttpServletRequest): ResponseEntity<ApiError> {
        log.warn("ejabberd API error on {} {}: {}", request.method, request.requestURI, e.message)
        return respond(HttpStatus.valueOf(e.statusCode.value()), "ejabberd_error", e.message ?: "ejabberd error")
    }

    /**
     * Maps bean-validation failures to 400 with per-field messages.
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val fields = e.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid value") }
        return ResponseEntity.badRequest().body(ApiError(400, "validation_error", "Request is invalid", fields))
    }

    /**
     * Maps unexpected errors to 500 Internal Server Error.
     */
    @ExceptionHandler(Exception::class)
    fun handleUnexpected(e: Exception, request: HttpServletRequest): ResponseEntity<ApiError> {
        log.error("Unexpected error handling {} {}", request.method, request.requestURI, e)
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "An unexpected error occurred")
    }

    /**
     * Builds the standard ApiError response body.
     */
    private fun respond(status: HttpStatus, code: String, message: String): ResponseEntity<ApiError> =
        ResponseEntity.status(status).body(ApiError(status.value(), code, message))
}
