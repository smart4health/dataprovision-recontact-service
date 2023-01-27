package com.healthmetrix.recontact.request.usecases

import com.healthmetrix.recontact.commons.RecontactRequest
import com.healthmetrix.recontact.commons.test.TestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class CohortInfoChangedEventProcessorTest {

    @Test
    fun `test sample json object mapping for compatibility`() {
        TestUtils().fromJson<RecontactRequest>("request.json")
    }

    @Test
    fun `toString test`() {
        val request = RecontactRequest(
            cohort = RecontactRequest.Cohort(
                name = "some-rp-specific-value",
                datasetVersion = "1.0",
                distribution = RecontactRequest.Cohort.Distribution(
                    age = listOf(
                        RecontactRequest.Cohort.AgeDistributionItem(min = 50, max = 59, count = 5),
                        RecontactRequest.Cohort.AgeDistributionItem(min = 40, max = 49, count = 5),
                        RecontactRequest.Cohort.AgeDistributionItem(count = 1),
                    ),
                    gender = listOf(
                        RecontactRequest.Cohort.GenderDistributionItem(gender = "MALE", count = 5),
                        RecontactRequest.Cohort.GenderDistributionItem(gender = "FEMALE", count = 6),
                    ),
                ),
                pseudonymIds = generateSequence(UUID::randomUUID).map(UUID::toString).take(20).toList(),
                queryParams = listOf(
                    RecontactRequest.Cohort.QueryParam(
                        listOf(
                            RecontactRequest.Cohort.QueryParam.Content(
                                "someFilter",
                                emptyList(),
                                true,
                            ),
                            RecontactRequest.Cohort.QueryParam.Content(
                                "someFilterB",
                                emptyList(),
                                true,
                            ),
                        ),
                    ),
                    RecontactRequest.Cohort.QueryParam(
                        listOf(
                            RecontactRequest.Cohort.QueryParam.Content(
                                "someOtherFilter",
                                listOf(
                                    RecontactRequest.Cohort.QueryParam.Content.Attribute(
                                        "patient.attributes.Gender",
                                        listOf("M", "F"),
                                    ),
                                ),
                                false,
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertThat(request.toCohortInfoMessage()).isEqualTo(
            """
            Size: 20
            Dataset version: 1.0
            
            Age distribution: 
            + [N/A - N/A]: 1
            + [40 - 49]: 5
            + [50 - 59]: 5
            
            Gender distribution: 
            + Female: 6
            + Male: 5
            
            Query parameters:
            + Filter Name: "someFilter" | Excluded: yes; + Filter Name: "someFilterB" | Excluded: yes
            + Filter Name: "someOtherFilter" | Excluded: no | Attributes: [patient.attributes.Gender: M, F]
            """.trimIndent(),
        )

        assertThat(
            request.copy(cohort = request.cohort.copy(queryParams = listOf())).toCohortInfoMessage(),
        ).isEqualTo(
            """
            Size: 20
            Dataset version: 1.0
            
            Age distribution: 
            + [N/A - N/A]: 1
            + [40 - 49]: 5
            + [50 - 59]: 5
            
            Gender distribution: 
            + Female: 6
            + Male: 5
            
            Query parameters:
            none
            """.trimIndent(),
        )
    }
}
