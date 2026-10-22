package dev.stevanmosquetti.openplaud.web

import dev.stevanmosquetti.openplaud.client.EjabberdException
import dev.stevanmosquetti.openplaud.service.ConflictException
import dev.stevanmosquetti.openplaud.service.InvalidSlugException
import dev.stevanmosquetti.openplaud.service.NotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    fun mapsNotFoundTo404() {
        val response = handler.handleNotFound(NotFoundException("Gone"))

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals(404, response.body!!.status)
        assertEquals("not_found", response.body!!.error)
        assertEquals("Gone", response.body!!.message)
    }

    @Test
    fun mapsConflictTo409() {
        val response = handler.handleConflict(ConflictException("Already here"))

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertEquals("conflict", response.body!!.error)
        assertEquals("Already here", response.body!!.message)
    }

    @Test
    fun mapsInvalidSlugTo422() {
        val response = handler.handleInvalidSlug(InvalidSlugException("Nope"))

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.statusCode)
        assertEquals("invalid_slug", response.body!!.error)
        assertEquals("Nope", response.body!!.message)
    }

    @Test
    fun proxiesUpstreamStatusForEjabberdErrors() {
        val request = MockHttpServletRequest()
        request.method = "POST"
        request.requestURI = "/api/v1/groups"

        val response =
            handler.handleEjabberd(EjabberdException("Server said no", HttpStatus.BAD_GATEWAY, "Bad Gateway"), request)

        assertEquals(HttpStatus.BAD_GATEWAY, response.statusCode)
        assertEquals("ejabberd_error", response.body!!.error)
        assertEquals("Server said no", response.body!!.message)
    }

    @Test
    fun mapsFieldErrorsToMessages() {
        val fieldError = FieldError("operator", "name", "Name is required")
        val bindingResult = Mockito.mock(BindingResult::class.java)
        Mockito.`when`(bindingResult.fieldErrors).thenReturn(listOf(fieldError))
        val method = GlobalExceptionHandlerTest::class.java.getDeclaredMethod("dummy", String::class.java)
        val exception = MethodArgumentNotValidException(MethodParameter(method, 0), bindingResult)

        val response = handler.handleValidation(exception)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        val body = response.body!!
        assertEquals("validation_error", body.error)
        assertEquals("Request is invalid", body.message)
        assertEquals("Name is required", body.fields["name"])
    }

    private fun dummy(@Suppress("UNUSED_PARAMETER") param: String) {}

    @Test
    fun mapsUnexpectedTo500() {
        val response = handler.handleUnexpected(RuntimeException("boom"), MockHttpServletRequest())

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals("internal_error", response.body!!.error)
        assertEquals("An unexpected error occurred", response.body!!.message)
        assertNull(response.body!!.fields["nonexistent"])
    }
}
