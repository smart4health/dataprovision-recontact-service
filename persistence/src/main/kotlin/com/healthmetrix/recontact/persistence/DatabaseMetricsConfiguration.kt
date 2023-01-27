package com.healthmetrix.recontact.persistence

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.db.DatabaseTableMetrics
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

/**
 * Metrics name = db.table.size
 */
@Configuration
class DatabaseMetricsConfiguration(
    private val registry: MeterRegistry,
    private val dataSource: DataSource,
) {

    @PostConstruct
    fun initializeTableSizeMetrics() {
        listOf("request", "message", "citizen_cohort").forEach { tableName ->
            DatabaseTableMetrics.monitor(registry, tableName, "recontact", dataSource)
        }
    }
}
