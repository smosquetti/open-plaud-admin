package dev.stevanmosquetti.openplaud.service

import dev.stevanmosquetti.openplaud.client.EjabberdClient
import dev.stevanmosquetti.openplaud.dto.CreateTempRoomResponse
import dev.stevanmosquetti.openplaud.repository.OperatorGroupRepository
import dev.stevanmosquetti.openplaud.repository.OperatorRepository
import dev.stevanmosquetti.openplaud.slugify.Slugifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Base64
import java.util.UUID

@Service
class TempRoomService(
    private val operatorGroupRepository: OperatorGroupRepository,
    private val operatorRepository: OperatorRepository,
    private val ejabberd: EjabberdClient,
    @Value("\${ejabberd.muc-service}") var mucService: String,
    @Value("\${ejabberd.username}") var username: String,
    @Value("\${ejabberd.password}") val password: String,
) {

    private val authorization: String
        get() = "Basic ${Base64.getEncoder().encodeToString("$username:$password".toByteArray())}"

    fun create(user: String, groupId: UUID, service: String?): CreateTempRoomResponse {
        val room = slugify(user)
        ejabberd.createRoom(room, service?: mucService, authorization)
        ejabberd.setRoomAffiliation(
            room,
            service?: mucService,
            user,
            "member",
            authorization)

        val operators: List<UUID> = operatorGroupRepository.listOperatorsByGroupId(groupId)
        operators
            .forEach { uuid ->
                ejabberd.setRoomAffiliation(
                    room,
                    service?: mucService,
                    operatorRepository.getUsernameById(uuid),
                    "member",
                    authorization)
            }

        return CreateTempRoomResponse(room, service?: mucService)
    }

    fun removeAllOtherOperators(operatorId: UUID, groupId: UUID, room: String, service: String?) {
        val operators: List<UUID> = operatorGroupRepository.listOperatorsByGroupId(groupId)
        operators
            .filter { uuid -> uuid != operatorId }
            .forEach { uuid ->
                ejabberd.setRoomAffiliation(
                    room,
                    service?: mucService,
                    operatorRepository.getUsernameById(uuid),
                    "none",
                    authorization)
            }
    }

    fun removeRoom(room: String, service: String?) {
        ejabberd.destroyRoom(room, service?: mucService, authorization)
    }

    private fun slugify(user: String): String = try {
        Slugifier.slugify(user)
    } catch (e: IllegalArgumentException) {
        throw InvalidSlugException(e.message ?: "The user does not produce a valid MUC room name")
    }

}