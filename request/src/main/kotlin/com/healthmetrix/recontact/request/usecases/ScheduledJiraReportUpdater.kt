package com.healthmetrix.recontact.request.usecases

import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.runCatching
import com.healthmetrix.recontact.commons.logger
import com.healthmetrix.recontact.commons.orThrow
import com.healthmetrix.recontact.jira.api.JiraApiClient
import com.healthmetrix.recontact.jira.api.JiraReport
import com.healthmetrix.recontact.persistence.message.api.Message
import com.healthmetrix.recontact.persistence.message.api.MessageRepository
import com.healthmetrix.recontact.persistence.request.api.RequestRepository
import io.micrometer.core.annotation.Timed
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.ZonedDateTime

@Component
class ScheduledJiraReportUpdater(
    private val messageRepository: MessageRepository,
    private val requestRepository: RequestRepository,
    private val jiraApiClient: JiraApiClient,
    @Value("\${jira.report-sync-rate}")
    private val fixedRateString: String,
) {

    @Scheduled(fixedRateString = "\${jira.report-sync-rate}")
    @Timed(value = "s4h.jira.scheduled.updater.timed", description = "Time taken for scheduled Jira Report Updater")
    fun generateAndUploadReportToJira() {
        logger.info("Starting scheduled Jira Report Updater")
        val fixedRateInMinutes = Duration.parse(fixedRateString).toMinutes()
        val requestIdsToUpdate = messageRepository
            .findAllUpdatedAfter(ZonedDateTime.now().minusMinutes(fixedRateInMinutes))
            .map { it.linkedRequest }.toSet()

        logger.info("Updating reports for IDs: [${requestIdsToUpdate.joinToString(", ")}]")

        requestIdsToUpdate.forEach { requestId ->
            runCatching {
                val request = requestRepository.findById(requestId)!!
                val messages = messageRepository.findAllByRequestId(request.id)
                val grouped = messages.groupingBy { it.state }.eachCount()
                val report = JiraReport(
                    cohortSize = request.cohort.citizens.size,
                    read = grouped[Message.State.READ] ?: 0,
                    delivered = (grouped[Message.State.DELIVERED] ?: 0) + (grouped[Message.State.READ] ?: 0),
                    total = messages.size,
                    active = request.active,
                )
                jiraApiClient
                    .updateReportField(requestId, report.toString())
                    .orThrow()
            }.onFailure {
                logger.warn("Failed to update Report for $requestId: ${it.javaClass.simpleName}")
            }
        }
    }
}
