package dev.stevanmosquetti.openplaud.client

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import tools.jackson.databind.ObjectMapper

/**
 * Thin client over ejabberd's mod_http_api (POST /api/<command>, Basic auth, flat JSON body).
 *
 * `set_room_affiliation` / `get_room_affiliation` fail with "Room doesn't exists".
 */
@Component
class EjabberdClient(
    @Value("\${ejabberd.base-url}") baseUrl: String,
    @Value("\${ejabberd.host}") val host: String,
) {

    private val restClient: RestClient = RestClient.builder()
        .baseUrl(baseUrl)
        .build()

    fun getAuthorizationHeader(): String? {
        val requestAttributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        val request: HttpServletRequest? = requestAttributes?.request
        return request?.getHeader(HttpHeaders.AUTHORIZATION)
    }

    fun registerUser(user: String, userPassword: String, authorization: String? = "") {
        postForStatus(
            "register",
            mapOf("user" to user, "host" to host, "password" to userPassword),
            getAuthorizationHeader()?: authorization,
        )
    }

    /**
     * Unregisters a user from the server (mod_http_api `unregister`). Used to compensate
     * a registration whose database row could not be persisted.
     */
    fun unregisterUser(user: String, authorization: String? = "") {
        postForStatus(
            "unregister",
            mapOf("user" to user, "host" to host),
            getAuthorizationHeader()?: authorization,
        )
    }

    fun createRoom(room: String, service: String, authorization: String? = "") {
        postForStatus(
            "create_room_with_opts",
            mapOf(
                "room" to room,
                "service" to service,
                "host" to host,
                "options" to listOf(mapOf("name" to "members_only", "value" to "true")),
            ),
            getAuthorizationHeader()?: authorization,
        )
    }

    fun createPersistentRoom(room: String, service: String, authorization: String? = "") {
        postForStatus(
            "create_room_with_opts",
            mapOf(
                "room" to room,
                "service" to service,
                "host" to host,
                "options" to listOf(
                    mapOf("name" to "persistent", "value" to "true"),
                    mapOf("name" to "members_only", "value" to "true")),
            ),
            getAuthorizationHeader()?: authorization,
        )
    }

    /**
     * Destroys a MUC room (mod_http_api `destroy_room`). Used to compensate a room
     * creation whose database row could not be persisted.
     * mapOf("room" to room, "service" to mucService, "host" to host),
     */
    fun destroyRoom(room: String, service: String, authorization: String? = "") {
        postForStatus(
            "destroy_room",
            mapOf("room" to room, "service" to service),
            getAuthorizationHeader()?: authorization,
        )
    }

    fun setRoomAffiliation(room: String, service: String, user: String, affiliation: String = "member", authorization: String? = "") {
        postForStatus(
            "set_room_affiliation",
            mapOf(
                "room" to room,
                "service" to service,
                "user" to user,
                "host" to host,
                "affiliation" to affiliation,
            ),
            getAuthorizationHeader()?: authorization,
        )
    }

    fun getRoomAffiliation(room: String, service: String, jid: String, authorization: String? = ""): String {
        return postForString(
            "get_room_affiliation",
            mapOf("room" to room, "service" to service, "jid" to jid),
            getAuthorizationHeader()?: authorization
        ).replace("\"", "")
    }

    private fun postForStatus(command: String, body: Any, authorization: String?) {
        restClient.post()
            .uri("/$command")
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, authorization?: "")
            .body(objectToJsonString(body))
            .exchange { _, response ->
                if (!response.statusCode.is2xxSuccessful()) {
                    throw errorFromResponse(response)
                }
            }
    }

    private fun postForString(command: String, body: Any, authorization: String?): String {
        return restClient.post()
            .uri("/$command")
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, authorization?: "")
            .body(objectToJsonString(body))
            .exchange { _, response ->
                if (!response.statusCode.is2xxSuccessful()) {
                    throw errorFromResponse(response)
                }
                response.bodyTo<String>(String::class.java) ?: ""
            }
    }

    private fun objectToJsonString(map: Any): String {
        val mapper: ObjectMapper = ObjectMapper()
        return mapper.writeValueAsString(map)
    }

    private fun errorFromResponse(response: ConvertibleClientHttpResponse): EjabberdException {
        val errorBody: Map<*, *>? = try {
            response.bodyTo(Map::class.java)
        } catch (e: Exception) {
            null
        }
        val message = (errorBody?.get("message") as? String) ?: "Unknown ejabberd error"
        return EjabberdException(message, response.statusCode, response.statusText)
    }
}
