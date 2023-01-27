package com.healthmetrix.recontact.jira.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.runCatching
import com.github.michaelbull.result.toResultOr
import com.healthmetrix.recontact.commons.RecontactRequest
import com.healthmetrix.recontact.commons.logger
import com.healthmetrix.recontact.commons.orThrow
import io.micrometer.core.annotation.Counted
import net.logstash.logback.argument.StructuredArguments
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient

const val INVALID_TEXT =
    "This issue contains invalid cohort info and has been invalidated. Please contact IT or create a new issue with the correct cohort file"
const val METRICS_NAME_JIRA_REPORT = "s4h.jira.update.report.field"
const val METRICS_NAME_JIRA_COHORT = "s4h.jira.update.cohort.field"
const val METRICS_NAME_JIRA_INVALIDATE = "s4h.jira.update.status.invalid"
const val METRICS_NAME_JIRA_FETCH_REQUEST = "s4h.jira.fetch.request"

/**
 * On Request and Message updates this client sends a generated update report to Jira which leads to updating
 * a customField visible in the ticket. For this the Api User needs to:
 * - have permissions to edit these tickets
 * - there needs to be one custom field available having parts of JiraConfigurationProperties#reportFieldName in its name (case-insensitive)
 * - this custom field needs to be in editable state
 *
 * All exceptions are wrapped in Results and have to be handled by the callers of this client.
 */
@Component
@Profile("jira")
class JiraApiClientImpl(
    private val webClient: WebClient,
    private val props: JiraApiConfigurationProperties,
    private val objectMapper: ObjectMapper,
    private val decryptRecontactRequestUseCase: DecryptRecontactRequestUseCase,
) : JiraApiClient {

    @Counted(value = METRICS_NAME_JIRA_REPORT, description = "Amount of Jira Report Field Updates")
    override fun updateReportField(id: String, report: String): Result<Unit, Throwable> = runCatching<Unit> {
        updateCustomFieldWithValue(id, props.reportFieldName, report)
    }.onFailure { logger.error("JiraApi: Updating the issue $id failed with {}: {}", it.javaClass, it.message) }

    @Counted(value = METRICS_NAME_JIRA_COHORT, description = "Amount of Jira Cohort Field Updates")
    override fun updateCohortInfoField(id: String, cohortInfo: String): Result<Unit, Throwable> = runCatching<Unit> {
        updateCustomFieldWithValue(id, props.cohortInfoFieldName, cohortInfo)
    }.onFailure { logger.error("JiraApi: Updating the issue $id failed with {}: {}", it.javaClass, it.message) }

    /**
     * 1. Get available state transitions for Issue
     * 2. Find "Invalid" state transitions id
     * 3. Transition issue to the state "Invalid"
     * 4. Add INVALID_TEXT note to report and cohort info fields
     */
    @Counted(value = METRICS_NAME_JIRA_INVALIDATE, description = "Amount of Jira Issues being invalidated")
    override fun invalidateIssue(id: String, reason: String?): Result<Unit, Throwable> = runCatching<Unit> {
        logger.info(
            "Invalidating issue {} {}",
            StructuredArguments.kv("id", id),
            StructuredArguments.kv("reason", reason),
        )
        val transitionsUri = "/issue/$id/transitions"
        val transitions = webClient.get().uri(transitionsUri)
            .retrieve()
            .bodyToMono(TransitionsResponse::class.java)
            .block()!!

        webClient.post().uri(transitionsUri)
            .body(BodyInserters.fromValue(transitions.toInvalidTransitionUpdate(props.invalidStatusName)))
            .retrieve()
            .toBodilessEntity()
            .block()

        // Setting note about invalidation on CohortInfo and Report field
        updateReportField(id, INVALID_TEXT)
        updateCohortInfoField(id, INVALID_TEXT)
    }.onFailure { logger.error("JiraApi: Updating the issue $id failed with {}: {}", it.javaClass, it.message) }

    /**
     * 1. Get Jira Ticket
     * 2. Search for binary attachments and decrypt. Fallback search for json attachments if enabled
     */
    @Counted(value = METRICS_NAME_JIRA_FETCH_REQUEST, description = "Amount of times the Jira issue is fetched")
    override fun getSignedRecontactRequest(issueId: String): Result<RecontactRequest, Throwable> = runCatching {
        val issue = webClient.get().uri("/issue/$issueId?expand=names,renderedFields")
            .retrieve()
            .bodyToMono(IssueResponse::class.java)
            .block()!!

        val (jsonFiles, binaryFiles) = (issue.renderedFields["attachment"] as ArrayNode)
            .map { objectMapper.treeToValue(it, Attachment::class.java) }
            .let { list ->
                list.filter { it.mimeType.contains("json") } to list.filter { it.mimeType.contains("octet-stream") }
            }

        when {
            binaryFiles.size == 1 -> {
                val cipherBytes = webClient.get().uri(binaryFiles.first().content)
                    .retrieve()
                    .bodyToMono(ByteArray::class.java)
                    .block()!!
                decryptRecontactRequestUseCase(cipherBytes)
                    .mapError { CohortInfoError.DecryptionError }
                    .orThrow()
            }
            jsonFiles.size == 1 && props.plaintextJsonAllowed -> {
                webClient.get().uri(jsonFiles.first().content)
                    .retrieve()
                    .bodyToMono(RecontactRequest::class.java)
                    .block()!!
            }
            binaryFiles.size > 1 || jsonFiles.size > 1 -> throw CohortInfoError.MoreThanOneFound
            else -> throw CohortInfoError.NotFound
        }
    }.onFailure {
        logger.error(
            "JiraApi: Fetching attachment for the issue $issueId failed with {}: {}",
            it.javaClass,
            it.message,
        )
    }

    private fun updateCustomFieldWithValue(issueId: String, fieldName: String, value: String) {
        val issue = webClient.get().uri("/issue/$issueId?expand=names,renderedFields")
            .retrieve()
            .bodyToMono(IssueResponse::class.java)
            .block()!!

        val fieldId = issue.findCustomField(fieldName).orThrow()

        webClient.put().uri("/issue/$issueId")
            .body(BodyInserters.fromValue(IssueUpdate(fields = mapOf(fieldId to value))))
            .retrieve()
            .toBodilessEntity()
            .block()
    }

    private fun IssueResponse.findCustomField(labelName: String): Result<String, Throwable> =
        names
            .filter { (id, label) ->
                id.startsWith("customfield") && renderedFields.containsKey(id) && label.lowercase()
                    .contains(labelName)
            }.keys
            .firstOrNull()
            .toResultOr { JiraError.CustomFieldNotFound("Custom field $labelName not part of the issue $id") }

    private fun TransitionsResponse.toInvalidTransitionUpdate(invalidStatusName: String) =
        TransitionUpdate(
            transition = TransitionUpdate.Transition(
                transitions.first {
                    it.to.name.lowercase().contains(invalidStatusName)
                }.id,
            ),
        )
}

sealed class CohortInfoError : Throwable() {
    object MoreThanOneFound : CohortInfoError()
    object NotFound : CohortInfoError()
    object DecryptionError : CohortInfoError()
}

sealed class JiraError(override val message: String) : Exception() {
    class CustomFieldNotFound(override val message: String) : JiraError(message)
}
