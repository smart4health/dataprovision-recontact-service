package com.healthmetrix.recontact.request.usecases

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.onFailure
import com.healthmetrix.recontact.commons.CohortInfoChanged
import com.healthmetrix.recontact.commons.kv
import com.healthmetrix.recontact.commons.logger
import com.healthmetrix.recontact.jira.api.CohortInfoError
import com.healthmetrix.recontact.jira.api.JiraApiClient
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class CohortInfoChangedEventProcessor(
    private val jiraApiClient: JiraApiClient,
) {
    @EventListener
    fun onEvent(event: CohortInfoChanged): Result<Unit, Throwable> = binding<Unit, Throwable> {
        logger.info(
            "Cohort Info Change received {}",
            "issueId" kv event.issueId,
        )

        val recontactRequest = jiraApiClient.getSignedRecontactRequest(event.issueId)
            .onFailure {
                val errorMessage = when (it) {
                    is CohortInfoError.NotFound -> "No Cohort Info file has been provided!"
                    is CohortInfoError.MoreThanOneFound -> "More than one Cohort Info file has been provided!"
                    is CohortInfoError.DecryptionError -> "The Cohort Info file could not be decrypted!"
                    else -> "The Cohort Info file could not be processed successfully!"
                }
                jiraApiClient.updateCohortInfoField(event.issueId, errorMessage)
            }
            .bind()

        jiraApiClient.updateCohortInfoField(event.issueId, recontactRequest.toCohortInfoMessage()).bind()
    }.onFailure {
        logger.warn(
            "Cohort Info Change Processing failed {} {}",
            "issueId" kv event.issueId,
            "throwable" kv it,
        )
    }
}
