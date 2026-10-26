package dev.stevanmosquetti.openplaud.service

import dev.stevanmosquetti.openplaud.client.EjabberdClient
import dev.stevanmosquetti.openplaud.dto.CreateTempRoomResponse
import dev.stevanmosquetti.openplaud.entity.Operator
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
        "conference"
    )

    private val groupId = UUID.randomUUID()
    private val operatorId = UUID.randomUUID()
    private val operator = Operator(operatorId, "Operator", "operator", "operator@email.com")
    private val otherOperatorA = Operator(UUID.randomUUID(), "otherOperatorA", "other_a", "othera@email.com")
    private val otherOperatorB = Operator(UUID.randomUUID(), "otherOperatorB", "other_b", "otherb@email.com")

    @Test
    fun createOpensRoomAndAffiliatesUserAndGroupOperators() {
        Mockito.`when`(operatorGroupRepository.listOperatorsByGroupId(groupId))
            .thenReturn(listOf(otherOperatorA, otherOperatorB))
        Mockito.`when`(operatorRepository.getUsernameById(otherOperatorA.id)).thenReturn("other_a")
        Mockito.`when`(operatorRepository.getUsernameById(otherOperatorB.id)).thenReturn("other_b")

        val response = service.create("John Doe", groupId, "support")

        val inOrder = Mockito.inOrder(ejabberd)
        inOrder.verify(ejabberd).createRoom("johndoe", "support")
        inOrder.verify(ejabberd).setRoomAffiliation("johndoe", "support", "John Doe", "member")
        inOrder.verify(ejabberd).setRoomAffiliation("johndoe", "support", "other_a", "admin")
        inOrder.verify(ejabberd).setRoomAffiliation("johndoe", "support", "other_b", "admin")
        assertEquals(CreateTempRoomResponse("johndoe", "support"), response)
    }

    @Test
    fun createFallsBackToDefaultMucServiceWhenServiceIsNull() {
        Mockito.`when`(operatorGroupRepository.listOperatorsByGroupId(groupId)).thenReturn(emptyList())

        val response = service.create("john.doe", groupId, null)

        val inOrder = Mockito.inOrder(ejabberd)
        inOrder.verify(ejabberd).createRoom("johndoe", "conference")
        inOrder.verify(ejabberd).setRoomAffiliation("johndoe", "conference", "john.doe", "member")
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
            .thenReturn(listOf(operator, otherOperatorA, otherOperatorB))
        Mockito.`when`(operatorRepository.getUsernameById(otherOperatorA.id)).thenReturn("other_a")
        Mockito.`when`(operatorRepository.getUsernameById(otherOperatorB.id)).thenReturn("other_b")

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
        Mockito.`when`(operatorRepository.getUsernameById(otherOperatorA.id)).thenReturn("other_a")

        service.removeAllOtherOperators(operatorId, groupId, "johndoe", null)

        Mockito.verify(ejabberd).setRoomAffiliation("johndoe", "conference", "other_a", "outcast")
    }

    @Test
    fun removeRoomDestroysRoomOnGivenService() {
        service.removeRoom("johndoe", "support")
        Mockito.verify(ejabberd).destroyRoom("johndoe", "support")
    }

    @Test
    fun removeRoomFallsBackToDefaultMucServiceWhenServiceIsNull() {
        service.removeRoom("johndoe", null)
        Mockito.verify(ejabberd).destroyRoom("johndoe", "conference")
    }
}
