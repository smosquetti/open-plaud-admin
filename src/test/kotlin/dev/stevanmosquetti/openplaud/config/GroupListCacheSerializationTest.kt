package dev.stevanmosquetti.openplaud.config

import dev.stevanmosquetti.openplaud.dto.GroupResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Guards the Redis cache value serializer: a cached `List<GroupResponse>` must
 * round-trip back to real [GroupResponse] instances, not `List<LinkedHashMap>`.
 *
 * This pins the two non-obvious requirements in [CacheConfig.valueSerializer]:
 * `defaultTyping` must be on (else list elements lose their type) and the
 * `ObjectMapper` must register the Kotlin module (else the data class cannot be
 * constructed).
 */
class GroupListCacheSerializationTest {

    @Test
    fun cachedGroupListRoundTripsToGroupResponse() {
        val groups = listOf(
            GroupResponse(UUID.randomUUID(), "Support Team", "supportteam", "conference"),
            GroupResponse(UUID.randomUUID(), "Sales", "sales", "support"),
        )

        val serializer = CacheConfig.valueSerializer()
        val bytes = serializer.serialize(groups)
        val back = serializer.deserialize(bytes) as List<GroupResponse>

        assertEquals(groups, back)
        assertTrue(back.all { it is GroupResponse }, "elements must be GroupResponse, got " + back.map { it::class.qualifiedName })
    }

    @Test
    fun emptyGroupListRoundTrips() {
        val serializer = CacheConfig.valueSerializer()
        val bytes = serializer.serialize(emptyList<GroupResponse>())
        val back = serializer.deserialize(bytes) as List<GroupResponse>
        assertTrue(back.isEmpty())
    }
}
