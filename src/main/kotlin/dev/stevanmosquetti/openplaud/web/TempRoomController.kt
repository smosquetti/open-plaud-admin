package dev.stevanmosquetti.openplaud.web

import dev.stevanmosquetti.openplaud.dto.CreateTempRoomRequest
import dev.stevanmosquetti.openplaud.dto.CreateTempRoomResponse
import dev.stevanmosquetti.openplaud.service.TempRoomService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class TempRoomController(
    private val tempRoomService: TempRoomService,
) {

    @PostMapping("/chatroom")
    fun createOperator(@Valid @RequestBody request: CreateTempRoomRequest): ResponseEntity<CreateTempRoomResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(tempRoomService.create(request.user, request.groupId, request.service))
    }
}
