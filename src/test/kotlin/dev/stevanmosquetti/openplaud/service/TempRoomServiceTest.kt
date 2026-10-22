package dev.stevanmosquetti.openplaud.service

import dev.stevanmosquetti.openplaud.client.EjabberdClient
import dev.stevanmosquetti.openplaud.dto.CreateTempRoomResponse
import dev.stevanmosquetti.openplaud.repository.OperatorGroupRepository
import dev.stevanmosquetti.openplaud.repository.OperatorRepository
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.Base64
import java.util.UUID

class TempRoomServiceTest {

    private val operatorGroupRepository = Mockito.mock(OperatorGroupRepository::class.java)
    private val operatorRepository = Mockito.mock(OperatorRepository::class.java)
    private val ejabberd = Mockito.mock(EjabberdClient::class.java)
    private val service = TempRoomService(
        operatorGroupRepository,
        operatorRepository,
        ejabberd,
        "conference",
        "admin@localhost",
        "admin",
    )

    private val authorization: String
        get() = "Basic " + Base64.getEncoder().encodeToString("admin@localhost:admin".toByteArray())

    private val groupId = UUID.randomUUID()
    private val operatorId = UUID.randomUUID()
    private val otherOperatorA = UUID.randomUUID()
    private val otherOperatorB = UUID.randomUUID()

    @Test
    fun createOpensRoomAndAffiliatesUserAndGroupOperators() {
        Mockito.`when`(operatorGroupRepository.listOperatorsByGroupId(groupId))
            .thenReturn(listOf(otherOperatorA, otherOperatorB))
        Mockito.`when`(operatorRepository.getUsernameById(otherOperatorA)).thenReturn("other_a")
        Mockito.`when`(operatorRepository.getUsernameById(otherOperatorB)).thenReturn("other_b")

        val response = service.create("John Doe", groupId, "support")

        val inOrder = Mockito.inOrder(ejabberd)
        inOrder.verify(ejabberd).createRoom("johndoe", "support", authorization)
        inOrder.verify(ejabberd).setRoomAffiliation("johndoe", "support", "John Doe", "member", authorization)
        inOrder.verify(ejabberd).setRoomAffiliation("johndoe", "support", "other_a", "admin", authorization)
        inOrder.verify(ejabberd).setRoomAffiliation("johndoe", "support", "other_b", "admin", authorization)
        assertEquals(CreateTempRoomResponse("johndoe", "support"), response)
    }

    @Test
    fun createFallsBackToDefaultMucServiceWhenServiceIsNull() {
        Mockito.`when`(operatorGroupRepository.listOperatorsByGroupId(groupId)).thenReturn(emptyList())

        val response = service.create("john.doe", groupId, null)

        val inOrder = Mockito.inOrder(ejabberd)
        inOrder.verify(ejabberd).createRoom("johndoe", "conference", authorization)
        inOrder.verify(ejabberd).setRoomAffiliation("johndoe", "conference", "john.doe", "member", authorization)
        assertEquals(CreateTempRoomResponse("johndoe", "conference"), response)
    }

    @Test
    fun createRejectsUserWithoutValidLocalpart() {
        assertFailsWith<InvalidSlugException> { service.create("a", groupId, null) }
        assertFailsWith<InvalidSlugException> { service.create("!!?", groupId, null) }
        Mockito.verifyNoInteractions(ejabberd)
    }

    @Test
    fun removeAllOtherOperatorsClearsAffiliationOnlyForOtherOperators() {
        Mockito.`when`(operatorGroupRepository.listOperatorsByGroupId(groupId))
            .thenReturn(listOf(operatorId, otherOperatorA, otherOperatorB))
        Mockito.`when`(operatorRepository.getUsernameById(otherOperatorA)).thenReturn("other_a")
        Mockito.`when`(operatorRepository.getUsernameById(otherOperatorB)).thenReturn("other_b")

        service.removeAllOtherOperators(operatorId, groupId, "johndoe", "support")

        val inOrder = Mockito.inOrder(ejabberd)
        inOrder.verify(ejabberd).setRoomAffiliation("johndoe", "support", "other_a", "outcast")
        inOrder.verify(ejabberd).setRoomAffiliation("johndoe", "support", "other_b", "outcast")
        Mockito.verify(operatorRepository, Mockito.never()).getUsernameById(operatorId)
        Mockito.verifyNoMoreInteractions(ejabberd)
    }

    @Test
    fun removeAllOtherOperatorsFallsBackToDefaultMucServiceWhenServiceIsNull() {
        Mockito.`when`(operatorGroupRepository.listOperatorsByGroupId(groupId)).thenReturn(listOf(otherOperatorA))
        Mockito.`when`(operatorRepository.getUsernameById(otherOperatorA)).thenReturn("other_a")

        service.removeAllOtherOperators(operatorId, groupId, "johndoe", null)

        Mockito.verify(ejabberd).setRoomAffiliation("johndoe", "conference", "other_a", "outcast")
    }

    @Test
    fun removeRoomDestroysRoomOnGivenService() {
        service.removeRoom("johndoe", "support")
        Mockito.verify(ejabberd).destroyRoom("johndoe", "support", authorization)
    }

    @Test
    fun removeRoomFallsBackToDefaultMucServiceWhenServiceIsNull() {
        service.removeRoom("johndoe", null)
        Mockito.verify(ejabberd).destroyRoom("johndoe", "conference", authorization)
    }
}
