package dev.stevanmosquetti.openplaud.service

import dev.stevanmosquetti.openplaud.client.EjabberdClient
import dev.stevanmosquetti.openplaud.repository.OperatorGroupRepository
import dev.stevanmosquetti.openplaud.repository.OperatorRepository
import dev.stevanmosquetti.openplaud.slugify.Slugifier
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TempRoomService(
    private val operatorGroupRepository: OperatorGroupRepository,
    private val operatorRepository: OperatorRepository,
    private val ejabberd: EjabberdClient,
) {

    fun create(jid: String, groupId: UUID, service: String) {
        val room = slugify(jid)
        ejabberd.createRoom(room, service)

        val operators: List<UUID> = operatorGroupRepository.listOperatorsByGroupId(groupId)
        operators
            .forEach { uuid ->
                ejabberd.setRoomAffiliation(room, service, operatorRepository.getUsernameById(uuid))
            }
    }

    fun removeAllOtherOperators(operatorId: UUID, groupId: UUID, room: String, service: String) {
        val operators: List<UUID> = operatorGroupRepository.listOperatorsByGroupId(groupId)
        operators
            .filter { uuid -> uuid != operatorId }
            .forEach { uuid ->
                ejabberd.setRoomAffiliation(room, service, operatorRepository.getUsernameById(uuid), "none")
            }
    }

    fun removeRoom(room: String, service: String) {
        ejabberd.destroyRoom(room, service)
    }

    private fun slugify(jid: String): String = try {
        Slugifier.slugify(jid)
    } catch (e: IllegalArgumentException) {
        throw InvalidSlugException(e.message ?: "The user jid does not produce a valid MUC room name")
    }

}