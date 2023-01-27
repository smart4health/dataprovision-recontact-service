package com.healthmetrix.recontact.jira.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import com.github.michaelbull.result.Result
import com.healthmetrix.recontact.commons.RecontactRequest

interface JiraApiClient {
    fun updateReportField(id: String, report: String): Result<Unit, Throwable>
    fun updateCohortInfoField(id: String, cohortInfo: String): Result<Unit, Throwable>
    fun invalidateIssue(id: String, reason: String?): Result<Unit, Throwable>
    fun getSignedRecontactRequest(issueId: String): Result<RecontactRequest, Throwable>
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class IssueResponse(
    val id: String,
    val key: String,
    val names: Map<String, String>,
    val renderedFields: Map<String, JsonNode>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TransitionsResponse(
    val transitions: List<Transition>,
) {
    data class Transition(
        val id: String,
        val name: String,
        val to: Status,
    )

    data class Status(
        val name: String,
        val id: String,
    )
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Attachment(
    val filename: String,
    val mimeType: String,
    val content: String,
)

data class IssueUpdate(
    val fields: Map<String, String>,
)

data class TransitionUpdate(
    val transition: Transition,
) {
    data class Transition(
        val id: String,
    )
}
