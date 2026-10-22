package dev.stevanmosquetti.openplaud.service

import dev.stevanmosquetti.openplaud.client.EjabberdClient
import dev.stevanmosquetti.openplaud.entity.Operator
import dev.stevanmosquetti.openplaud.repository.OperatorGroupRepository
import dev.stevanmosquetti.openplaud.repository.OperatorRepository
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class OperatorServiceTest {

    private val operatorRepository = Mockito.mock(OperatorRepository::class.java)
    private val operatorGroupRepository = Mockito.mock(OperatorGroupRepository::class.java)
    private val ejabberd = Mockito.mock(EjabberdClient::class.java)
    private lateinit var service: OperatorService

    @BeforeEach
    fun setUp() {
        service = OperatorService(operatorRepository, operatorGroupRepository, ejabberd)
    }

    @Test
    fun registersXmppUserBeforePersistingOperator() {
        Mockito.`when`(operatorRepository.findAll()).thenReturn(emptyList())
        val saved = Operator(name = "John Doe", email = "johndoe@email.com", username = "johndoe")
        Mockito.`when`(operatorRepository.save(Mockito.any(Operator::class.java))).thenReturn(saved)

        val response = service.register("John Doe", "johndoe@email.com", "pw123")

        val inOrder = Mockito.inOrder(ejabberd, operatorRepository, operatorGroupRepository)
        inOrder.verify(ejabberd).registerUser("johndoe", "pw123")
        inOrder.verify(operatorRepository).save(Mockito.any(Operator::class.java))
        assertEquals(saved.id, response.id)
        assertEquals("John Doe", response.name)
        assertEquals("johndoe@email.com", response.email)
        assertEquals("johndoe", response.xmppUser)
        assertEquals("pw123", response.initialPassword)
    }

    @Test
    fun generatesPasswordWhenOmitted() {
        Mockito.`when`(operatorRepository.findAll()).thenReturn(emptyList())
        val saved = Operator(name = "John Doe", email = "johndoe@email.com", username = "johndoe")
        Mockito.`when`(operatorRepository.save(Mockito.any(Operator::class.java))).thenReturn(saved)

        val response = service.register("John Doe", "johndoe@email.com", null)

        val generated = checkNotNull(response.initialPassword)
        Mockito.verify(ejabberd).registerUser("johndoe", generated)
        assertEquals(24, generated.length)
    }

    @Test
    fun rejectsOperatorWithTakenXmppUser() {
        Mockito.`when`(operatorRepository.findAll()).thenReturn(listOf(
            Operator(name = "John Doe", email = "johndoe@email.com", username = "johndoe")))

        assertFailsWith<ConflictException> { service.register("john doe", "johndoe@email.com", "pw") }

        Mockito.verifyNoInteractions(ejabberd)
        Mockito.verify(operatorRepository, Mockito.never()).save(Mockito.any(Operator::class.java))
    }

    @Test
    fun rejectsNameWithoutValidLocalpart() {
        assertFailsWith<InvalidSlugException> { service.register("a", "email", "pw") }
        assertFailsWith<InvalidSlugException> { service.register("!!?", "email", "pw") }
        Mockito.verifyNoInteractions(ejabberd)
    }

    @Test
    fun unregistersXmppUserWhenPersistenceFails() {
        Mockito.`when`(operatorRepository.findAll()).thenReturn(emptyList())
        val dbError = RuntimeException("database down")
        Mockito.`when`(operatorRepository.save(Mockito.any(Operator::class.java))).thenThrow(dbError)

        val error = assertFailsWith<RuntimeException> { service.register("John Doe", "johndoe@email.com", "pw") }

        assertSame(dbError, error)
        val inOrder = Mockito.inOrder(ejabberd)
        inOrder.verify(ejabberd).registerUser("johndoe", "pw")
        inOrder.verify(ejabberd).unregisterUser("johndoe")
    }
}
