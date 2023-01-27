package com.healthmetrix.recontact.jira.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import com.healthmetrix.recontact.commons.RecontactRequest
import com.healthmetrix.recontact.commons.orThrow
import com.healthmetrix.recontact.commons.test.TestUtils
import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

class JiraApiClientImplTest {

    private val webClient: WebClient = mockk()
    private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
    private val decryptRecontactRequestUseCase: DecryptRecontactRequestUseCase = mockk()

    private val underTest =
        JiraApiClientImpl(
            webClient,
            JiraApiConfigurationProperties("url", "report", "invalid", "cohort info", true),
            objectMapper,
            decryptRecontactRequestUseCase,
        )

    private val requestBodyUriSpecMock: WebClient.RequestBodyUriSpec = mockk()
    private val getRequestBodySpecMock: WebClient.RequestBodySpec = mockk()
    private val putRequestBodySpecMock: WebClient.RequestBodySpec = mockk()
    private val getRequestHeadersUriSpecMock: WebClient.RequestHeadersUriSpec<*> = mockk()
    private val getResponseSpecMock: WebClient.ResponseSpec = mockk()
    private val putResponseSpecMock: WebClient.ResponseSpec = mockk()

    private val id = "10000"
    private val testUtils = TestUtils()
    private val uri = "/issue/$id"
    private val issue = testUtils.fromJson<IssueResponse>("issue-10000.json")

    @Test
    fun `update the report with success`() {
        getMocks(issue)
        putMocks()
        underTest.updateReportField(id, "new report data").orThrow()
    }

    @Test
    fun `update where field name changed`() {
        getMocks(issue.copy(names = mapOf("customfield_123456" to "My Report Label")))
        putMocks()
        underTest.updateReportField(id, "new report data")
    }

    @Test
    fun `update the report with get request failing`() {
        every { webClient.get() } returns getRequestHeadersUriSpecMock
        every {
            getRequestHeadersUriSpecMock.uri("$uri?expand=names,renderedFields")
        } returns getRequestBodySpecMock
        every { getRequestBodySpecMock.retrieve() } throws webClientException()

        underTest.updateReportField(id, "new report data").getError().also {
            assertThat(it).isNotNull
            assertThat(it).isInstanceOf(WebClientResponseException::class.java)
        }
    }

    @Test
    fun `update the report with put request failing`() {
        getMocks(issue)
        every { webClient.put() } returns requestBodyUriSpecMock
        every { requestBodyUriSpecMock.uri(uri) } returns putRequestBodySpecMock
        every { putRequestBodySpecMock.body(any()) } returns putRequestBodySpecMock
        every { putRequestBodySpecMock.retrieve() } throws webClientException()

        underTest.updateReportField(id, "new report data").getError().also {
            assertThat(it).isNotNull
            assertThat(it).isInstanceOf(WebClientResponseException::class.java)
        }
    }

    @Test
    fun `update the report with finding field failing`() {
        getMocks(issue.copy(names = mapOf("customfield_bla" to "nothing")))

        underTest.updateReportField(id, "new report data").getError().also {
            assertThat(it).isNotNull
            assertThat(it).isInstanceOf(JiraError.CustomFieldNotFound::class.java)
        }
    }

    @Test
    fun `update the report with field names not in the response`() {
        getMocks(issue.copy(names = mapOf()))

        underTest.updateReportField(id, "new report data").getError().also {
            assertThat(it).isNotNull
            assertThat(it).isInstanceOf(JiraError.CustomFieldNotFound::class.java)
        }
    }

    @Test
    fun `get request from jira attachment and decrypt`() {
        val issueWithEncAndJsonAttachments =
            testUtils.fromJson<IssueResponse>("issue-with-enc-and-json-attachment.json")
        val encrypted = ByteArray(0)
        val expected = mockRequest()
        every { webClient.get() } returns getRequestHeadersUriSpecMock
        every {
            getRequestHeadersUriSpecMock.uri("$uri?expand=names,renderedFields")
        } returns getRequestBodySpecMock
        every { getRequestHeadersUriSpecMock.uri(match<String> { it.endsWith(".enc") }) } returns getRequestBodySpecMock
        every { getRequestBodySpecMock.retrieve() } returns getResponseSpecMock
        every { getResponseSpecMock.bodyToMono(IssueResponse::class.java) } returns Mono.just(
            issueWithEncAndJsonAttachments,
        )
        every { getResponseSpecMock.bodyToMono(ByteArray::class.java) } returns Mono.just(encrypted)
        every { decryptRecontactRequestUseCase(encrypted) } returns Ok(expected)

        val actual = underTest.getSignedRecontactRequest(id).get()!!

        assertThat(actual).isEqualTo(expected)
        verify { getRequestHeadersUriSpecMock.uri(match<String> { it.endsWith(".json") }) wasNot called }
    }

    @Test
    fun `get request from jira attachment and use json`() {
        val issueWithOnlyJsonAttachment =
            testUtils.fromJson<IssueResponse>("issue-with-json-attachment.json")
        val expected = mockRequest()
        every { webClient.get() } returns getRequestHeadersUriSpecMock
        every {
            getRequestHeadersUriSpecMock.uri("$uri?expand=names,renderedFields")
        } returns getRequestBodySpecMock
        every { getRequestHeadersUriSpecMock.uri(match<String> { it.endsWith(".json") }) } returns getRequestBodySpecMock
        every { getRequestBodySpecMock.retrieve() } returns getResponseSpecMock
        every { getResponseSpecMock.bodyToMono(IssueResponse::class.java) } returns Mono.just(
            issueWithOnlyJsonAttachment,
        )
        every { getResponseSpecMock.bodyToMono(RecontactRequest::class.java) } returns Mono.just(expected)

        val actual = underTest.getSignedRecontactRequest(id).get()!!

        assertThat(actual).isEqualTo(expected)
        verify { decryptRecontactRequestUseCase wasNot called }
        verify { getRequestHeadersUriSpecMock.uri(match<String> { it.endsWith(".enc") }) wasNot called }
    }

    @Test
    fun `get request from jira attachment and use json, but no json allowed`() {
        val issueWithOnlyJsonAttachment =
            testUtils.fromJson<IssueResponse>("issue-with-json-attachment.json")

        every { webClient.get() } returns getRequestHeadersUriSpecMock
        every {
            getRequestHeadersUriSpecMock.uri("$uri?expand=names,renderedFields")
        } returns getRequestBodySpecMock
        every { getRequestHeadersUriSpecMock.uri(match<String> { it.endsWith(".json") }) } returns getRequestBodySpecMock
        every { getRequestBodySpecMock.retrieve() } returns getResponseSpecMock
        every { getResponseSpecMock.bodyToMono(IssueResponse::class.java) } returns Mono.just(
            issueWithOnlyJsonAttachment,
        )
        every { getResponseSpecMock.bodyToMono(RecontactRequest::class.java) } returns Mono.just(mockRequest())

        val underTest = JiraApiClientImpl(
            webClient,
            JiraApiConfigurationProperties("url", "report", "invalid", "cohort info", false),
            objectMapper,
            decryptRecontactRequestUseCase,
        )
        underTest.getSignedRecontactRequest(id).also {
            assertThat(it.getError()).isEqualTo(CohortInfoError.NotFound)
        }
        verify { decryptRecontactRequestUseCase wasNot called }
    }

    private fun webClientException() = WebClientResponseException("errormessage", 400, "error", null, null, null)

    private fun getMocks(issue: IssueResponse) {
        every { webClient.get() } returns getRequestHeadersUriSpecMock
        every {
            getRequestHeadersUriSpecMock.uri("$uri?expand=names,renderedFields")
        } returns getRequestBodySpecMock
        every { getRequestBodySpecMock.retrieve() } returns getResponseSpecMock
        every { getResponseSpecMock.bodyToMono(IssueResponse::class.java) } returns Mono.just(issue)
    }

    private fun putMocks() {
        every { webClient.put() } returns requestBodyUriSpecMock
        every { requestBodyUriSpecMock.uri(uri) } returns putRequestBodySpecMock
        every { putRequestBodySpecMock.body(any()) } returns putRequestBodySpecMock
        every { putRequestBodySpecMock.retrieve() } returns putResponseSpecMock
        every { putResponseSpecMock.toBodilessEntity() } returns Mono.empty()
    }

    private fun mockRequest(): RecontactRequest = RecontactRequest(
        cohort = RecontactRequest.Cohort(
            name = "some-rp-specific-value",
            datasetVersion = "1.0",
            distribution = RecontactRequest.Cohort.Distribution(age = listOf(), gender = listOf()),
            pseudonymIds = listOf(),
            queryParams = listOf(),
        ),
    )
}
