package dev.stevanmosquetti.openplaud.web

import dev.stevanmosquetti.openplaud.dto.CreateOperatorRequest
import dev.stevanmosquetti.openplaud.dto.OperatorResponse
import dev.stevanmosquetti.openplaud.service.OperatorService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class OperatorController(
    private val operatorService: OperatorService,
) {

    /**
     * Registers an operator: creates the XMPP user on ejabberd and inserts a row into
     * the `operators` table. The XMPP login password is returned in the response.
     */
    @PostMapping("/operators")
    fun createOperator(@Valid @RequestBody request: CreateOperatorRequest): ResponseEntity<OperatorResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(operatorService.register(request.name, request.password))
}
