package dev.stevanmosquetti.openplaud.web

import dev.stevanmosquetti.openplaud.dto.CreateTempRoomRequest
import dev.stevanmosquetti.openplaud.dto.CreateTempRoomResponse
import dev.stevanmosquetti.openplaud.dto.JoinChatroomRequest
import dev.stevanmosquetti.openplaud.service.TempRoomService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.http.HttpStatus
import java.util.UUID

class TempRoomControllerTest {

    private val tempRoomService = Mockito.mock(TempRoomService::class.java)
    private val controller = TempRoomController(tempRoomService)

    @Test
    fun createOperatorReturnsCreatedWithServiceResult() {
        val groupId = UUID.randomUUID()
        val expected = CreateTempRoomResponse("johndoe", "conference")
        Mockito.`when`(tempRoomService.create("john.doe", groupId, "conference")).thenReturn(expected)

        val response = controller.createChatroom(CreateTempRoomRequest("john.doe", groupId, "conference"))

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals(expected, response.body)
        Mockito.verify(tempRoomService).create("john.doe", groupId, "conference")
    }

    @Test
    fun createOperatorForwardsNullServiceToTempRoomService() {
        val groupId = UUID.randomUUID()
        val expected = CreateTempRoomResponse("johndoe", "conference")
        Mockito.`when`(tempRoomService.create("john.doe", groupId, null)).thenReturn(expected)

        val response = controller.createChatroom(CreateTempRoomRequest("john.doe", groupId))

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals(expected, response.body)
        Mockito.verify(tempRoomService).create("john.doe", groupId, null)
    }

    @Test
    fun joinReturnsOkAndOutcastsOtherOperators() {
        val operatorId = UUID.randomUUID()
        val groupId = UUID.randomUUID()

        val response = controller.joinChatroom(
            JoinChatroomRequest(operatorId, groupId, "johndoe", "support"),
        )

        assertEquals(HttpStatus.OK, response.statusCode)
        Mockito.verify(tempRoomService).removeAllOtherOperators(operatorId, groupId, "johndoe", "support")
    }

    @Test
    fun joinForwardsNullServiceToTempRoomService() {
        val operatorId = UUID.randomUUID()
        val groupId = UUID.randomUUID()

        controller.joinChatroom(JoinChatroomRequest(operatorId, groupId, "johndoe"))

        Mockito.verify(tempRoomService).removeAllOtherOperators(operatorId, groupId, "johndoe", null)
    }
}
