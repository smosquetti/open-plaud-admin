package dev.stevanmosquetti.openplaud.web

import dev.stevanmosquetti.openplaud.dto.CreateTempRoomRequest
import dev.stevanmosquetti.openplaud.dto.CreateTempRoomResponse
import dev.stevanmosquetti.openplaud.dto.JoinChatroomRequest
import dev.stevanmosquetti.openplaud.dto.TransferRequest
import dev.stevanmosquetti.openplaud.service.TempRoomService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/chatroom")
class TempRoomController(
    private val tempRoomService: TempRoomService,
) {

    /**
     * Creates a temp MUC room for `user` (affiliation `member`) and makes every
     * operator of the group an admin of the room.
     */
    @PostMapping()
    fun createChatroom(@Valid @RequestBody request: CreateTempRoomRequest): ResponseEntity<CreateTempRoomResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(tempRoomService.create(request.user, request.name, request.groupId, request.service))
    }
    
    /**
     * Joins a temp room: all *other* operators of the group are set to `outcast` so
     * only the joining operator keeps access.
     */
    @PostMapping("/join")
    fun joinChatroom(@Valid @RequestBody request: JoinChatroomRequest): ResponseEntity<Unit> {
        tempRoomService.removeAllOtherOperators(request.operatorId, request.groupId, request.room, request.service)
        return ResponseEntity.status(HttpStatus.OK).build()
    }

    @PostMapping("/transfer")
    fun transferToAnotherOperator(@Valid @RequestBody request: TransferRequest): ResponseEntity<Unit> {
        tempRoomService.transferToAnotherOperator(request.room, request.operatorId, request.previousOperatorId, request.service)
        return ResponseEntity.status(HttpStatus.OK).build()
    }
}
