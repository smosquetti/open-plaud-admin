package dev.stevanmosquetti.openplaud.service

import dev.stevanmosquetti.openplaud.client.EjabberdClient
import dev.stevanmosquetti.openplaud.entity.Group
import dev.stevanmosquetti.openplaud.entity.Operator
import dev.stevanmosquetti.openplaud.entity.OperatorGroup
import dev.stevanmosquetti.openplaud.entity.OperatorGroupId
import dev.stevanmosquetti.openplaud.repository.GroupRepository
import dev.stevanmosquetti.openplaud.repository.OperatorGroupRepository
import dev.stevanmosquetti.openplaud.repository.OperatorRepository
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import java.util.Optional
import java.util.UUID

class GroupServiceTest {

    private val groupRepository = Mockito.mock(GroupRepository::class.java)
    private val operatorRepository = Mockito.mock(OperatorRepository::class.java)
    private val operatorGroupRepository = Mockito.mock(OperatorGroupRepository::class.java)
    private val ejabberd = Mockito.mock(EjabberdClient::class.java)
    private lateinit var service: GroupService

    private val operatorId = UUID.randomUUID()
    private val groupId = UUID.randomUUID()
    private val operator = Operator(name = "John Doe", id = operatorId, username = "johndoe", email = "johndoe@email.com")
    private val group = Group(name = "Support Team", node = "supportteam", service = "supportservice", id = groupId)

    @BeforeEach
    fun setUp() {
        service = GroupService(groupRepository, operatorRepository, operatorGroupRepository, ejabberd)
    }

    @Test
    fun createsRoomBeforePersistingGroup() {
        Mockito.`when`(groupRepository.findAll()).thenReturn(emptyList())
        Mockito.`when`(groupRepository.save(Mockito.any(Group::class.java))).thenReturn(group)

        val response = service.create("Support Team", "supportservice")

        val inOrder = Mockito.inOrder(ejabberd, groupRepository)
        inOrder.verify(ejabberd).createPersistentRoom("supportteam", "supportservice")
        inOrder.verify(groupRepository).save(Mockito.any(Group::class.java))
        assertEquals(group.id, response.id)
        assertEquals("Support Team", response.name)
        assertEquals("supportteam", response.room)
        assertEquals("supportservice", response.service)
    }

    @Test
    fun rejectsGroupWithTakenRoom() {
        Mockito.`when`(groupRepository.findAll()).thenReturn(listOf(
            Group(name = "Support Team", node = "supportteam", service = "supportservice")))

        assertFailsWith<ConflictException> { service.create("support team", "supportservice") }

        Mockito.verifyNoInteractions(ejabberd)
    }

    @Test
    fun rejectsGroupNameWithoutValidRoom() {
        assertFailsWith<InvalidSlugException> { service.create("a", "supportservice") }
        assertFailsWith<InvalidSlugException> { service.create("!!?", "supportservice") }
        Mockito.verifyNoInteractions(ejabberd)
    }

    @Test
    fun destroysRoomWhenPersistenceFails() {
        Mockito.`when`(groupRepository.findAll()).thenReturn(emptyList())
        val dbError = RuntimeException("database down")
        Mockito.`when`(groupRepository.save(Mockito.any(Group::class.java))).thenThrow(dbError)

        val error = assertFailsWith<RuntimeException> { service.create("Support Team", "supportservice") }

        assertSame(dbError, error)
        val inOrder = Mockito.inOrder(ejabberd)
        inOrder.verify(ejabberd).createPersistentRoom("supportteam", "supportservice")
        inOrder.verify(ejabberd).destroyRoom("supportteam", "supportservice")
    }

    @Test
    fun addOperatorSetsMemberAffiliationThenPersistsMembership() {
        Mockito.`when`(operatorRepository.findById(operatorId)).thenReturn(Optional.of(operator))
        Mockito.`when`(groupRepository.findById(groupId)).thenReturn(Optional.of(group))
        Mockito.`when`(operatorGroupRepository.existsByOperatorIdAndGroupId(operatorId, groupId)).thenReturn(false)

        service.addOperatorToGroup(groupId, operatorId)

        val inOrder = Mockito.inOrder(ejabberd, operatorGroupRepository)
        inOrder.verify(ejabberd).setRoomAffiliation("supportteam", "supportservice", "johndoe", "member")
        inOrder.verify(operatorGroupRepository).save(Mockito.any(OperatorGroup::class.java))

        val captor = ArgumentCaptor.forClass(OperatorGroup::class.java)
        Mockito.verify(operatorGroupRepository).save(captor.capture())
        assertEquals(operatorId, captor.value.id.operatorId)
        assertEquals(groupId, captor.value.id.groupId)
    }

    @Test
    fun addOperatorRejectsUnknownOperator() {
        Mockito.`when`(operatorRepository.findById(operatorId)).thenReturn(Optional.empty())

        assertFailsWith<NotFoundException> { service.addOperatorToGroup(groupId, operatorId) }

        Mockito.verifyNoInteractions(ejabberd)
    }

    @Test
    fun addOperatorRejectsUnknownGroup() {
        Mockito.`when`(operatorRepository.findById(operatorId)).thenReturn(Optional.of(operator))
        Mockito.`when`(groupRepository.findById(groupId)).thenReturn(Optional.empty())

        assertFailsWith<NotFoundException> { service.addOperatorToGroup(groupId, operatorId) }

        Mockito.verifyNoInteractions(ejabberd)
    }

    @Test
    fun addOperatorRejectsDuplicateMembership() {
        Mockito.`when`(operatorRepository.findById(operatorId)).thenReturn(Optional.of(operator))
        Mockito.`when`(groupRepository.findById(groupId)).thenReturn(Optional.of(group))
        Mockito.`when`(operatorGroupRepository.existsByOperatorIdAndGroupId(operatorId, groupId)).thenReturn(true)

        assertFailsWith<ConflictException> { service.addOperatorToGroup(groupId, operatorId) }

        Mockito.verifyNoInteractions(ejabberd)
    }

    @Test
    fun addOperatorRevertsAffiliationWhenPersistenceFails() {
        Mockito.`when`(operatorRepository.findById(operatorId)).thenReturn(Optional.of(operator))
        Mockito.`when`(groupRepository.findById(groupId)).thenReturn(Optional.of(group))
        Mockito.`when`(operatorGroupRepository.existsByOperatorIdAndGroupId(operatorId, groupId)).thenReturn(false)
        val dbError = RuntimeException("database down")
        Mockito.`when`(operatorGroupRepository.save(Mockito.any(OperatorGroup::class.java))).thenThrow(dbError)

        val error = assertFailsWith<RuntimeException> { service.addOperatorToGroup(groupId, operatorId) }

        assertSame(dbError, error)
        val inOrder = Mockito.inOrder(ejabberd)
        inOrder.verify(ejabberd).setRoomAffiliation("supportteam", "supportservice", "johndoe", "member")
        inOrder.verify(ejabberd).setRoomAffiliation("supportteam", "supportservice", "johndoe", "none")
    }

    @Test
    fun removeOperatorClearsAffiliationThenDeletesRow() {
        val key = OperatorGroupId(operatorId, groupId)
        val membership = OperatorGroup(key)
        Mockito.`when`(operatorGroupRepository.findById(key)).thenReturn(Optional.of(membership))
        Mockito.`when`(operatorRepository.findById(operatorId)).thenReturn(Optional.of(operator))
        Mockito.`when`(groupRepository.findById(groupId)).thenReturn(Optional.of(group))

        service.removeOperatorFromGroup(groupId, operatorId)

        val inOrder = Mockito.inOrder(ejabberd, operatorGroupRepository)
        inOrder.verify(ejabberd).setRoomAffiliation("supportteam", "supportservice", "johndoe", "none")
        inOrder.verify(operatorGroupRepository).deleteById(key)
    }

    @Test
    fun removeOperatorRejectsUnknownMembership() {
        val key = OperatorGroupId(operatorId, groupId)
        Mockito.`when`(operatorGroupRepository.findById(key)).thenReturn(Optional.empty())

        assertFailsWith<NotFoundException> { service.removeOperatorFromGroup(groupId, operatorId) }

        Mockito.verifyNoInteractions(ejabberd)
    }
}
