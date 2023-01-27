package com.healthmetrix.recontact.request.controller

import com.healthmetrix.recontact.commons.ApiResponse
import com.healthmetrix.recontact.commons.CohortInfoChanged
import com.healthmetrix.recontact.commons.asEntity
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Before a request is approved or cancelled, it doesn't exist on our domain yet (and shouldn't).
 * However we need to update certain issue information once the cohort info has been added/changed.
 */
@RestController
class JiraIssueEventController(
    private val applicationEventPublisher: ApplicationEventPublisher,
) {

    @PostMapping("/v1/jira/issue/{issueId}")
    fun processJiraEvent(
        @PathVariable("issueId")
        issueId: String,
        @RequestParam("event", required = true)
        event: String,
    ): ResponseEntity<JiraIssueCreatedResponse> {
        when (event.toJiraIssueEvent()) {
            JiraIssueEvent.COHORT_INFO -> applicationEventPublisher.publishEvent(CohortInfoChanged(issueId))
            else -> Unit
        }
        return JiraIssueCreatedResponse.Success.asEntity()
    }

    sealed class JiraIssueCreatedResponse : ApiResponse {
        object Success : JiraIssueCreatedResponse() {
            override val status = HttpStatus.OK
        }
    }
}

enum class JiraIssueEvent(val label: String) {
    COHORT_INFO("cohortinfo"),
}

fun String.toJiraIssueEvent(): JiraIssueEvent? =
    JiraIssueEvent.values().firstOrNull { it.label.equals(other = this, ignoreCase = true) }
