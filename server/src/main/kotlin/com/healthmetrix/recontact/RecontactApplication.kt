package com.healthmetrix.recontact

import org.springframework.boot.actuate.autoconfigure.metrics.data.RepositoryMetricsAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Exclusions:
 * - DataSourceAutoConfiguration: see com.healthmetrix.recontact.persistence.DatabaseConfiguration
 * - RepositoryMetricsAutoConfiguration: we don't need crud repositories being auto timed for now
 */
@SpringBootApplication(exclude = [DataSourceAutoConfiguration::class, RepositoryMetricsAutoConfiguration::class])
@ConfigurationPropertiesScan
@EnableScheduling
class RecontactApplication

fun main(args: Array<String>) {
    runApplication<RecontactApplication>(*args)
}
