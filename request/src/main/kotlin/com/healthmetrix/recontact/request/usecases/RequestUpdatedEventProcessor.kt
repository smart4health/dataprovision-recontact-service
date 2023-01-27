package com.healthmetrix.recontact.request.usecases

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.toResultOr
import com.healthmetrix.recontact.commons.RequestUpdatedEvent
import com.healthmetrix.recontact.commons.kv
import com.healthmetrix.recontact.commons.logger
import com.healthmetrix.recontact.jira.api.JiraApiClient
import com.healthmetrix.recontact.jira.api.JiraReport
import com.healthmetrix.recontact.persistence.message.api.Message
import com.healthmetrix.recontact.persistence.message.api.MessageRepository
import com.healthmetrix.recontact.persistence.request.api.RequestRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class RequestUpdatedEventProcessor(
    private val messageRepository: MessageRepository,
    private val requestRepository: RequestRepository,
    private val jiraApiClient: JiraApiClient,
    @Value("\${jira.update-report-immediately}")
    private val updateReportImmediately: Boolean,
) {
    @EventListener
    fun onEvent(event: RequestUpdatedEvent): Result<Unit, Throwable> = binding<Unit, Throwable> {
        logger.info(
            "Request Update received {} {}",
            "requestId" kv event.requestId,
            "type" kv event.updateType,
        )
        if (!updateReportImmediately) {
            return@binding
        }
        val request = requestRepository.findById(requestId = event.requestId)
            .toResultOr { RequestNotFound }
            .bind()
        val messages = messageRepository.findAllByRequestId(request.id)
        val grouped = messages.groupingBy { it.state }.eachCount()
        val report = JiraReport(
            cohortSize = request.cohort.citizens.size,
            read = grouped[Message.State.READ] ?: 0,
            delivered = (grouped[Message.State.DELIVERED] ?: 0) + (grouped[Message.State.READ] ?: 0),
            total = messages.size,
            active = request.active,
        )

        jiraApiClient.updateReportField(event.requestId, report.toString()).bind()
    }.onFailure {
        logger.warn(
            "Request Update Processing failed {} {} {}",
            "requestId" kv event.requestId,
            "type" kv event.updateType,
            "throwable" kv it,
        )
    }
}

private object RequestNotFound : Exception()
