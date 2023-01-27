package com.healthmetrix.recontact.request.usecases

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onFailure
import com.healthmetrix.recontact.jira.api.JiraApiClient
import com.healthmetrix.recontact.persistence.request.api.Request
import com.healthmetrix.recontact.persistence.request.api.RequestRepository
import com.healthmetrix.recontact.request.controller.CreateRequestData
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
class CreateRequestFromRemoteUseCase(
    private val requestRepository: RequestRepository,
    private val updateCohortMessagesUseCase: UpdateCohortMessagesUseCase,
    private val jiraApiClient: JiraApiClient,
) {
    @Transactional
    operator fun invoke(data: CreateRequestData): Result<Request, CreateRequestFromRemoteError> = binding {
        requestRepository.findById(data.id)?.let { Err(CreateRequestFromRemoteError.Duplicate).bind<Unit>() }

        val recontactRequest = jiraApiClient.getSignedRecontactRequest(data.id)
            .onFailure { jiraApiClient.invalidateIssue(data.id, it.message) }
            .mapError { CreateRequestFromRemoteError.JiraCohortInfo }
            .bind()

        val request = Request(
            id = data.id,
            cohort = Request.Cohort(citizens = recontactRequest.cohort.pseudonymIds.toSet(), name = "name"),
            active = true,
            createdAt = ZonedDateTime.now(),
            updatedAt = ZonedDateTime.now(),
            message = Request.Message(content = Request.Message.Content(text = data.message, title = data.title)),
        )

        requestRepository.upsert(request)
        updateCohortMessagesUseCase(request)
        request
    }
}

sealed class CreateRequestFromRemoteError {
    object Duplicate : CreateRequestFromRemoteError()
    object JiraCohortInfo : CreateRequestFromRemoteError()
}
