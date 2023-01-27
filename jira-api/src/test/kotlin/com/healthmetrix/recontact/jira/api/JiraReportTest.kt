package com.healthmetrix.recontact.jira.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class JiraReportTest {

    private val utcTime = ZonedDateTime.of(
        LocalDateTime.of(2020, 5, 10, 14, 12, 55),
        ZoneId.of("UTC"),
    )

    @Test
    fun `to String test for active request`() {
        assertThat(JiraReport(49, 63, 80, 81, utcTime, true).toString()).isEqualTo(
            """
            Cohort size: 81
            78.75 % of messages were delivered.
            61.25 % of messages were read.
            
            This report was last updated on May 10, 2020, 16:12 (CEST).
            """.trimIndent(),
        )
    }

    @Test
    fun `to String test for cancelled request`() {
        assertThat(JiraReport(49, 63, 80, 81, utcTime, false).toString()).isEqualTo(
            """
            Cohort size: 81
            Communication cancelled! All delivered/read messages were deleted.
            
            This report was last updated on May 10, 2020, 16:12 (CEST).
            """.trimIndent(),
        )
    }

    @Test
    fun `zeros test`() {
        assertThat(JiraReport(0, 0, 0, 50, utcTime, true).toString()).isEqualTo(
            """
            Cohort size: 50
            0.00 % of messages were delivered.
            0.00 % of messages were read.
            
            This report was last updated on May 10, 2020, 16:12 (CEST).
            """.trimIndent(),
        )
    }
}
