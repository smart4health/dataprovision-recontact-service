package com.healthmetrix.recontact.persistence

import com.healthmetrix.recontact.commons.Secrets
import com.healthmetrix.recontact.persistence.dao.CitizenCohortDao
import com.healthmetrix.recontact.persistence.dao.MessageDao
import com.healthmetrix.recontact.persistence.dao.RequestDao
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.postgres.PostgresPlugin
import org.jdbi.v3.sqlobject.kotlin.KotlinSqlObjectPlugin
import org.jdbi.v3.sqlobject.kotlin.onDemand
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
import javax.sql.DataSource

@Configuration
class DatabaseConfiguration {

    @Bean
    fun providePostgresDataSource(
        dataSourceProperties: DataSourceProperties,
        @Value("\${secrets.rds-credentials}")
        credentialsLocation: String,
        secrets: Secrets,
    ): DataSource = TransactionAwareDataSourceProxy(
        dataSourceProperties.apply {
            url = "jdbc:${secrets[credentialsLocation]}"
        }.initializeDataSourceBuilder().build(),
    )

    @Bean
    fun provideJdbi(
        dataSource: DataSource,
    ): Jdbi {
        return Jdbi.create(dataSource).also { jdbi ->
            jdbi.installPlugin(KotlinPlugin())
            jdbi.installPlugin(PostgresPlugin())
            jdbi.installPlugin(KotlinSqlObjectPlugin())
        }
    }

    @Bean
    fun messageDao(jdbi: Jdbi): MessageDao = jdbi.onDemand()

    @Bean
    fun requestDao(jdbi: Jdbi): RequestDao = jdbi.onDemand()

    @Bean
    fun citizenCohortDao(jdbi: Jdbi): CitizenCohortDao = jdbi.onDemand()
}
