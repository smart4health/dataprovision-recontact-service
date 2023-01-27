package com.healthmetrix.recontact.commons

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest

interface Secrets {
    operator fun get(key: String): String // missing secrets are fatal
}

@Component
@Profile("secrets-aws")
internal class AwsSecrets(
    private val secretsManagerClient: SecretsManagerClient,
) : Secrets {

    private val cache = mutableMapOf<String, String>()

    override fun get(key: String): String {
        return cache[key] ?: run {
            GetSecretValueRequest.builder()
                .secretId(key)
                .build()
                .let(secretsManagerClient::getSecretValue)
                .secretString()
                .also { cache[key] = it }
        }
    }
}

@Component
@Profile("secrets-vault")
internal class VaultSecrets(
    private val jiraSecrets: SecretsConfiguration.JiraSecrets,
    private val rdsCredentials: SecretsConfiguration.RdsCredentials,
    private val objectMapper: ObjectMapper,
) : Secrets {

    override operator fun get(key: String): String = when {
        key.contains("rds-credentials") -> rdsCredentials.url
        key.contains("jira-credentials") -> jiraSecrets.credentials.let(objectMapper::writeValueAsString)
        key.contains("shared-cohort-key") -> jiraSecrets.encryption.sharedCohortKey
        else -> throw Exception("Secret not configured!")
    }
}

@Configuration
internal class SecretsConfiguration {

    @Bean
    @Profile("secrets-aws")
    fun provideSecretsManagerClient(): SecretsManagerClient =
        SecretsManagerClient.builder().build()

    @ConfigurationProperties("rds-credentials")
    @Profile("secrets-vault")
    data class RdsCredentials(
        val url: String,
    )

    @ConfigurationProperties("jira-secrets")
    @Profile("secrets-vault")
    data class JiraSecrets(
        val encryption: Encryption,
        val credentials: Credentials,
    ) {
        data class Credentials(val username: String, val password: String)
        data class Encryption(val sharedCohortKey: String)
    }
}

/**
 * Can be used locally to mock remote secret paths. See application.yaml:
 * recontact/dev/rds-credentials/recontact -> rds-credentials.recontact
 */
@Component
@Profile("!secrets-aws & !secrets-vault")
internal class MockSecrets(
    private val env: Environment,
) : Secrets {
    override fun get(key: String): String =
        (listOf("mock-secrets") + key.split("/").drop(2))
            .joinToString(".")
            .let(env::getProperty)!!
}
