package com.healthmetrix.recontact.jira.api

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.healthmetrix.recontact.commons.RecontactRequest
import io.micrometer.core.annotation.Counted
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@Profile("!jira")
class JiraApiMockClient : JiraApiClient {

    @Counted(value = METRICS_NAME_JIRA_REPORT, description = "Amount of Jira Report Field Updates")
    override fun updateReportField(id: String, report: String): Result<Unit, Throwable> = Ok(Unit)

    @Counted(value = METRICS_NAME_JIRA_COHORT, description = "Amount of Jira Cohort Field Updates")
    override fun updateCohortInfoField(id: String, cohortInfo: String): Result<Unit, Throwable> = Ok(Unit)

    @Counted(value = METRICS_NAME_JIRA_INVALIDATE, description = "Amount of Jira Issues being invalidated")
    override fun invalidateIssue(id: String, reason: String?): Result<Unit, Throwable> = Ok(Unit)

    @Counted(value = METRICS_NAME_JIRA_FETCH_REQUEST, description = "Amount of times the Jira issue is fetched")
    override fun getSignedRecontactRequest(issueId: String): Result<RecontactRequest, Throwable> = Ok(fakeRequest())

    private fun fakeRequest(): RecontactRequest = RecontactRequest(
        cohort = RecontactRequest.Cohort(
            name = "some-rp-specific-value",
            datasetVersion = "1.0",
            distribution = RecontactRequest.Cohort.Distribution(
                age = listOf(RecontactRequest.Cohort.AgeDistributionItem(min = 40, max = 49, count = 5)),
                gender = listOf(
                    RecontactRequest.Cohort.GenderDistributionItem(gender = "MALE", count = 5),
                    RecontactRequest.Cohort.GenderDistributionItem(gender = "FEMALE", count = 6),
                ),
            ),
            pseudonymIds = generateSequence(UUID::randomUUID).map(UUID::toString).take(20).toList(),
            queryParams = listOf(),
        ),
    )
}
