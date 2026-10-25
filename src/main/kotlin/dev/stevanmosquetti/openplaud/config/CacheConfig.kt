package dev.stevanmosquetti.openplaud.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import java.time.Duration

/**
 * Redis-backed Spring cache abstraction for the app.
 *
 * The value serializer is a `GenericJackson2JsonRedisSerializer` with `defaultTyping`
 * enabled so cached values round-trip to their concrete types. Without type hints a
 * `List<GroupResponse>` deserializes back as a `List<LinkedHashMap>` and the DTOs are
 * unusable; and the `ObjectMapper` must register the Kotlin module (Jackson 2),
 * otherwise constructor-based Kotlin data classes like `GroupResponse` cannot be
 * instantiated. See [valueSerializer].
 */
@Configuration
@EnableCaching
class CacheConfig(private val connectionFactory: RedisConnectionFactory) {

    @Bean
    fun cacheManager(): RedisCacheManager {
        val defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .computePrefixWith { cacheName -> "openplaud:$cacheName:" }
            .entryTtl(Duration.ofMinutes(5))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer())
            )
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .build()
    }

    companion object {
        /**
         * The cache value serializer. `defaultTyping` and the Kotlin-module
         * `ObjectMapper` are required for cached values (e.g. `List<GroupResponse>`)
         * to round-trip back to their concrete types. Exposed for testing.
         */
        fun valueSerializer(): GenericJackson2JsonRedisSerializer =
            GenericJackson2JsonRedisSerializer.builder()
                .defaultTyping(true)
                .objectMapper(ObjectMapper().registerKotlinModule())
                .build()
    }
}
