package dev.stevanmosquetti.openplaud.client

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import tools.jackson.databind.ObjectMapper
import kotlin.io.encoding.Base64

/**
 * Thin client over ejabberd's mod_http_api (POST /api/<command>, Basic auth, flat JSON body).
 *
 * `set_room_affiliation` / `get_room_affiliation` fail with "Room doesn't exists".
 */
@Component
class EjabberdClient(
    @Value("\${ejabberd.base-url}") baseUrl: String,
    @Value("\${ejabberd.host}") val host: String,
    @Value("\${ejabberd.username}") val adminUsername: String,
    @Value("\${ejabberd.password}") val adminPassword: String,
) {

    private val log = LoggerFactory.getLogger(EjabberdClient::class.java)
    
    private val authorization: String
        get() = "Basic ${java.util.Base64.getEncoder().encodeToString("$adminUsername:$adminPassword".toByteArray())}"

    private val restClient: RestClient = RestClient.builder()
        .baseUrl(baseUrl)
        .build()

    fun getAuthorizationHeader(): String? {
        val requestAttributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        val request: HttpServletRequest? = requestAttributes?.request
        return request?.getHeader(HttpHeaders.AUTHORIZATION)
    }

    fun registerUser(user: String, userPassword: String) {
        log.debug("Registering XMPP user '{}'", user)
        postForStatus(
            "register",
            mapOf("user" to user, "host" to host, "password" to userPassword)
        )
    }

    /**
     * Unregisters a user from the server (mod_http_api `unregister`). Used to compensate
     * a registration whose database row could not be persisted.
     */
    fun unregisterUser(user: String) {
        log.debug("Unregistering XMPP user '{}'", user)
        postForStatus(
            "unregister",
            mapOf("user" to user, "host" to host)
        )
    }

    fun createRoom(room: String, service: String) {
        log.debug("Creating MUC room '{}' on service '{}'", room, service)
        postForStatus(
            "create_room_with_opts",
            mapOf(
                "room" to room,
                "service" to service,
                "host" to host,
                "options" to listOf(mapOf("name" to "members_only", "value" to "true")),
            )
        )
    }

    fun createPersistentRoom(room: String, service: String) {
        log.debug("Creating persistent MUC room '{}' on service '{}'", room, service)
        postForStatus(
            "create_room_with_opts",
            mapOf(
                "room" to room,
                "service" to service,
                "host" to host,
                "options" to listOf(
                    mapOf("name" to "persistent", "value" to "true"),
                    mapOf("name" to "members_only", "value" to "true")),
            )
        )
    }

    /**
     * Destroys a MUC room (mod_http_api `destroy_room`). Used to compensate a room
     * creation whose database row could not be persisted.
     * mapOf("room" to room, "service" to mucService, "host" to host),
     */
    fun destroyRoom(room: String, service: String) {
        log.debug("Destroying MUC room '{}' on service '{}'", room, service)
        postForStatus(
            "destroy_room",
            mapOf("room" to room, "service" to service)
        )
    }

    fun setRoomAffiliation(room: String, service: String, user: String, affiliation: String = "member") {
        log.debug("Setting affiliation '{}' for user '{}' in room '{}' on service '{}'", affiliation, user, room, service)
        postForStatus(
            "set_room_affiliation",
            mapOf(
                "room" to room,
                "service" to service,
                "user" to user,
                "host" to host,
                "affiliation" to affiliation,
            )
        )
    }

    fun getRoomAffiliation(room: String, service: String, jid: String): String {
        log.debug("Getting affiliation for jid '{}' in room '{}' on service '{}'", jid, room, service)
        return postForString(
            "get_room_affiliation",
            mapOf("room" to room, "service" to service, "jid" to jid)
        ).replace("\"", "")
    }
    
    fun checkOperatorAccess(): String {
        val credentials = extractCredentialsFromHeader()
        return postForString(
            "check_password",
            mapOf(
                "user" to credentials["user"], 
                "host" to host, 
                "password" to credentials["password"]
            )
        ).replace("\"", "")
    }
    
    fun checkAdminAccess(): String {
        val credentials = extractCredentialsFromHeader()
        if (credentials["user"] != adminUsername) {
            throw EjabberdException("Authentication failure", HttpStatusCode.valueOf(401), "Unauthorized")
        }
        return postForString(
            "check_password",
            mapOf(
                "user" to credentials["user"], 
                "host" to host, 
                "password" to credentials["password"]
            )
        ).replace("\"", "")
    }
    
    private fun extractCredentialsFromHeader(): Map<String, String> {
        val auth = getAuthorizationHeader() ?: throw EjabberdException(
            "Authentication failure",
            HttpStatusCode.valueOf(401),
            "Unauthorized"
        )
        val decodedBytes: ByteArray = Base64.decode(auth.split(" ")[1])
        val decodedString = decodedBytes.decodeToString()
        val user = decodedString.split(":")[0]
        val password = decodedString.split(":")[1]
        return mapOf("user" to user, "password" to password)
    }

    private fun postForStatus(command: String, body: Any) {
        restClient.post()
            .uri("/$command")
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader()?: authorization)
            .body(objectToJsonString(body))
            .exchange { _, response ->
                if (!response.statusCode.is2xxSuccessful()) {
                    throw errorFromResponse(command, response)
                }
            }
    }

    private fun postForString(command: String, body: Any): String {
        return restClient.post()
            .uri("/$command")
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, getAuthorizationHeader()?: authorization)
            .body(objectToJsonString(body))
            .exchange { _, response ->
                if (!response.statusCode.is2xxSuccessful()) {
                    throw errorFromResponse(command, response)
                }
                response.bodyTo<String>(String::class.java) ?: ""
            }
    }

    private fun objectToJsonString(map: Any): String {
        val mapper: ObjectMapper = ObjectMapper()
        return mapper.writeValueAsString(map)
    }

    private fun errorFromResponse(command: String, response: ConvertibleClientHttpResponse): EjabberdException {
        val errorBody: Map<*, *>? = try {
            response.bodyTo(Map::class.java)
        } catch (e: Exception) {
            null
        }
        val message = (errorBody?.get("message") as? String) ?: "Unknown ejabberd error"
        log.warn(
            "ejabberd mod_http_api command '{}' failed: HTTP {} ({}): {}",
            command, response.statusCode, response.statusText, message,
        )
        return EjabberdException(message, response.statusCode, response.statusText)
    }
}
