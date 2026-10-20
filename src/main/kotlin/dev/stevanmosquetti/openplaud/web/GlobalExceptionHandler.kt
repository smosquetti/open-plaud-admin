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

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(e: NotFoundException): ResponseEntity<ApiError> =
        respond(HttpStatus.NOT_FOUND, "not_found", e.message ?: "Resource not found")

    @ExceptionHandler(ConflictException::class)
    fun handleConflict(e: ConflictException): ResponseEntity<ApiError> =
        respond(HttpStatus.CONFLICT, "conflict", e.message ?: "Conflict")

    @ExceptionHandler(InvalidSlugException::class)
    fun handleInvalidSlug(e: InvalidSlugException): ResponseEntity<ApiError> =
        respond(HttpStatus.UNPROCESSABLE_ENTITY, "invalid_slug", e.message ?: "Invalid display name")

    @ExceptionHandler(EjabberdException::class)
    fun handleEjabberd(e: EjabberdException, request: HttpServletRequest): ResponseEntity<ApiError> {
        log.warn("ejabberd API error on {} {}: {}", request.method, request.requestURI, e.message)
        return respond(HttpStatus.BAD_GATEWAY, "ejabberd_error", e.message ?: "ejabberd error")
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val fields = e.fieldErrors.associate { it.field to (it.defaultMessage ?: "Invalid value") }
        return ResponseEntity.badRequest().body(ApiError(400, "validation_error", "Request is invalid", fields))
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(e: Exception, request: HttpServletRequest): ResponseEntity<ApiError> {
        log.error("Unexpected error handling {} {}", request.method, request.requestURI, e)
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "An unexpected error occurred")
    }

    private fun respond(status: HttpStatus, code: String, message: String): ResponseEntity<ApiError> =
        ResponseEntity.status(status).body(ApiError(status.value(), code, message))
}
