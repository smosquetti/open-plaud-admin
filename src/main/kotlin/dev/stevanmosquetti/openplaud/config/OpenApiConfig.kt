package dev.stevanmosquetti.openplaud.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun customOpenAPI(
        @Value("\${springdoc.info.title}") apiTitle: String,
        @Value("\${springdoc.info.version}") apiVersion: String,
        @Value("\${springdoc.info.description}") apiDescription: String
    ): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title(apiTitle)
                    .version(apiVersion)
                    .description(apiDescription)
            )
    }
}
