package dev.stevanmosquetti.openplaud.service

import dev.stevanmosquetti.openplaud.client.EjabberdClient
import dev.stevanmosquetti.openplaud.dto.OperatorResponse
import dev.stevanmosquetti.openplaud.entity.Operator
import dev.stevanmosquetti.openplaud.repository.OperatorRepository
import dev.stevanmosquetti.openplaud.slugify.Slugifier
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.security.SecureRandom

/**
 * Registers operators: a new XMPP user on ejabberd and a row in the `operators` table.
 *
 * The ejabberd call happens before the database write. If the database write fails, the
 * freshly registered XMPP user is unregistered again so the database stays authoritative.
 */
@Service
class OperatorService(
    private val operatorRepository: OperatorRepository,
    private val ejabberd: EjabberdClient,
) {

    private val log = LoggerFactory.getLogger(OperatorService::class.java)

    fun register(name: String, password: String?): OperatorResponse {
        val xmppUser = slugify(name)
        requireXmppUserFree(xmppUser)
        val actualPassword = password ?: generatePassword()
        ejabberd.registerUser(xmppUser, actualPassword)
        val operator = try {
            operatorRepository.save(Operator(name = name))
        } catch (dbError: RuntimeException) {
            cleanupAfterDbFailure(xmppUser, dbError)
            throw dbError
        }
        return OperatorResponse(
            id = operator.id,
            name = operator.name,
            xmppUser = xmppUser,
            initialPassword = actualPassword,
        )
    }

    private fun requireXmppUserFree(xmppUser: String) {
        val taken = operatorRepository.findAll().any { Slugifier.slugify(it.name) == xmppUser }
        if (taken) {
            throw ConflictException("An operator already exists for the XMPP user '$xmppUser'")
        }
    }

    private fun cleanupAfterDbFailure(xmppUser: String, cause: RuntimeException) {
        log.error("Could not persist operator '$xmppUser'; unregistering the XMPP user", cause)
        try {
            ejabberd.unregisterUser(xmppUser)
        } catch (e: Exception) {
            log.error("Compensating unregister for '$xmppUser' also failed; the user is orphaned on the XMPP server", e)
        }
    }

    private fun slugify(name: String): String = try {
        Slugifier.slugify(name)
    } catch (e: IllegalArgumentException) {
        throw InvalidSlugException(e.message ?: "The operator name does not produce a valid XMPP user")
    }

    companion object {
        private const val PASSWORD_LENGTH = 24
        private const val PASSWORD_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        private val secureRandom = SecureRandom()

        fun generatePassword(): String {
            val chars = CharArray(PASSWORD_LENGTH)
            for (i in chars.indices) {
                chars[i] = PASSWORD_ALPHABET[secureRandom.nextInt(PASSWORD_ALPHABET.length)]
            }
            return String(chars)
        }
    }
}
