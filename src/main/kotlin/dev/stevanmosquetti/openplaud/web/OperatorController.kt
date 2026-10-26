package dev.stevanmosquetti.openplaud.web

import dev.stevanmosquetti.openplaud.dto.CreateOperatorRequest
import dev.stevanmosquetti.openplaud.dto.GroupResponse
import dev.stevanmosquetti.openplaud.dto.OperatorResponse
import dev.stevanmosquetti.openplaud.service.OperatorService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class OperatorController(
    private val operatorService: OperatorService,
) {

    /**
     * Lists all operators from the database. The service verifies the caller is the
     * configured admin account before reading.
     */
    @GetMapping("/operators")
    fun getListOfOperators(): ResponseEntity<List<OperatorResponse>> {
        return ResponseEntity.status(HttpStatus.OK).body(operatorService.getListOfOperators())
    }

    @GetMapping("/operators/{operatorId}/groups")
    fun getListOfGroupsForTheOperator(@PathVariable operatorId: UUID): ResponseEntity<List<GroupResponse>> {
        return ResponseEntity.status(HttpStatus.OK).body(operatorService.getListOfGroupsForTheOperator(operatorId))
    }

    /**
     * Registers an operator: creates the XMPP user on ejabberd and inserts a row into
     * the `operators` table. The XMPP login password is returned in the response.
     */
    @PostMapping("/operators")
    fun createOperator(@Valid @RequestBody request: CreateOperatorRequest): ResponseEntity<OperatorResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(operatorService.register(request.name, request.email, request.password))

    /**
     * Removes an operator: deletes its membership rows and `operators` row, and
     * unregisters the XMPP user.
     */
    @DeleteMapping("/operators/{operatorId}")
    fun deleteOperator(@PathVariable operatorId: UUID): ResponseEntity<Unit> {
        operatorService.removeOperator(operatorId)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }
    
    /**
     * Verifies the caller's operator credentials against ejabberd `check_password`.
     */
    @GetMapping("/operator/login")
    fun checkPassword(): ResponseEntity<Unit> {
        operatorService.checkAccess()
        return ResponseEntity.status(HttpStatus.OK).build()
    }
}
