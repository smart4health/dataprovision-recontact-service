package com.healthmetrix.recontact.jira.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.healthmetrix.recontact.commons.Secrets
import com.healthmetrix.recontact.commons.logger
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.netty.Connection
import reactor.netty.http.client.HttpClient
import java.time.Duration
import java.util.concurrent.TimeUnit

@Profile("jira")
@ConfigurationProperties("jira")
data class JiraApiConfigurationProperties(
    val baseUrl: String,
    val reportFieldName: String,
    val cohortInfoFieldName: String,
    val invalidStatusName: String,
    val plaintextJsonAllowed: Boolean,
)

@Configuration
@Profile("jira")
class JiraApiClientConfiguration {

    @Bean
    fun jiraCredentialsProvider(
        objectMapper: ObjectMapper,
        secrets: Secrets,
        @Value("\${secrets.jira-credentials}")
        credentialsLocation: String,
    ): ApiCredentials = secrets[credentialsLocation].let { objectMapper.readValue(it, ApiCredentials::class.java) }

    @Bean
    fun webClient(
        props: JiraApiConfigurationProperties,
        apiCredentials: ApiCredentials,
    ): WebClient {
        if (apiCredentials.username.isBlank() || apiCredentials.password.isBlank()) {
            throw Exception("Empty Jira ApiSecrets")
        }
        val httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .responseTimeout(Duration.ofMillis(5000))
            .doOnConnected { conn: Connection ->
                conn.addHandlerLast(ReadTimeoutHandler(5000, TimeUnit.MILLISECONDS))
                    .addHandlerLast(WriteTimeoutHandler(5000, TimeUnit.MILLISECONDS))
            }
            // Jira attachment download needs this
            .followRedirect(true)

        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .baseUrl(props.baseUrl)
            .defaultHeaders {
                it.setBasicAuth(apiCredentials.username, apiCredentials.password)
                it.contentType = MediaType.APPLICATION_JSON
            }
            .filter(logRequest())
            .filter(logResponse())
            .build()
    }

    private fun logRequest(): ExchangeFilterFunction =
        ExchangeFilterFunction.ofRequestProcessor { clientRequest: ClientRequest ->
            logger.info("Request {}: {} {}", clientRequest.logPrefix(), clientRequest.method(), clientRequest.url())
            Mono.just(clientRequest)
        }

    private fun logResponse(): ExchangeFilterFunction =
        ExchangeFilterFunction.ofResponseProcessor { clientResponse: ClientResponse ->
            logger.info("Response {}: {}", clientResponse.logPrefix(), clientResponse.statusCode())
            Mono.just(clientResponse)
        }
}

data class ApiCredentials(
    val username: String,
    val password: String,
)
