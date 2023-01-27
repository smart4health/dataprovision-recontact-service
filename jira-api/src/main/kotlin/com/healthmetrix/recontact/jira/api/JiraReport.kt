package com.healthmetrix.recontact.jira.api

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale.ENGLISH

data class JiraReport(
    val read: Int,
    val delivered: Int,
    val total: Int,
    val cohortSize: Int,
    val reportTime: ZonedDateTime = ZonedDateTime.now(),
    val active: Boolean,
) {
    override fun toString(): String = if (active) {
        """
            Cohort size: $cohortSize
            ${getPercent(delivered, total)} % of messages were delivered.
            ${getPercent(read, total)} % of messages were read.
            
            This report was last updated on ${reportTime.formatToCentralEuropeanTime()}.
        """.trimIndent()
    } else {
        """
            Cohort size: $cohortSize
            Communication cancelled! All delivered/read messages were deleted.
            
            This report was last updated on ${reportTime.formatToCentralEuropeanTime()}.
        """.trimIndent()
    }

    private fun getPercent(read: Int, total: Int): String {
        return if (read == 0 || total == 0) {
            "%.2f".format(ENGLISH, 0.0)
        } else {
            "%.2f".format(ENGLISH, read / total.toDouble() * 100)
        }
    }
}

private fun ZonedDateTime.formatToCentralEuropeanTime(): String =
    this.withZoneSameInstant(ZoneId.of("CET")).format(DateTimeFormatter.ofPattern("MMMM d, yyyy, HH:mm (z)"))
