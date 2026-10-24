package dev.stevanmosquetti.openplaud.service

import dev.stevanmosquetti.openplaud.client.EjabberdClient
import dev.stevanmosquetti.openplaud.dto.CreateTempRoomResponse
import dev.stevanmosquetti.openplaud.repository.OperatorGroupRepository
import dev.stevanmosquetti.openplaud.repository.OperatorRepository
import dev.stevanmosquetti.openplaud.slugify.Slugifier
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TempRoomService(
    private val operatorGroupRepository: OperatorGroupRepository,
    private val operatorRepository: OperatorRepository,
    private val ejabberd: EjabberdClient,
    @Value("\${ejabberd.muc-service}") var mucService: String,
) {

    private val log = LoggerFactory.getLogger(TempRoomService::class.java)

    fun create(user: String, groupId: UUID, service: String?): CreateTempRoomResponse {
        val room = slugify(user)
        ejabberd.createRoom(room, service?: mucService)
        ejabberd.setRoomAffiliation(
            room,
            service?: mucService,
            user,
            "member")

        val operators: List<UUID> = operatorGroupRepository.listOperatorsByGroupId(groupId)
        operators
            .forEach { uuid ->
                ejabberd.setRoomAffiliation(
                    room,
                    service?: mucService,
                    operatorRepository.getUsernameById(uuid),
                    "admin")
            }

        log.info("Created temporary MUC room '$room' for user '$user' in service '${service?: mucService}'")
        return CreateTempRoomResponse(room, service?: mucService)
    }

    fun removeAllOtherOperators(operatorId: UUID, groupId: UUID, room: String, service: String?) {
        val operators: List<UUID> = operatorGroupRepository.listOperatorsByGroupId(groupId)
        val otherOperators = operators.filter { uuid -> uuid != operatorId }
        otherOperators
            .forEach { uuid ->
                ejabberd.setRoomAffiliation(
                    room,
                    service?: mucService,
                    operatorRepository.getUsernameById(uuid),
                    "outcast")
            }
        log.info("Operator $operatorId joined room '$room'; outcast ${otherOperators.size} other operator(s) of group $groupId")
    }

    fun removeRoom(room: String, service: String?) {
        ejabberd.destroyRoom(room, service?: mucService)
        log.info("Destroyed temporary MUC room '$room' in service '${service?: mucService}'")
    }

    private fun slugify(user: String): String = try {
        Slugifier.slugify(user)
    } catch (e: IllegalArgumentException) {
        throw InvalidSlugException(e.message ?: "The user does not produce a valid MUC room name")
    }

}