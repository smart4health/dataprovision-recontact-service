package com.healthmetrix.recontact.request.usecases

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrapError
import com.healthmetrix.recontact.commons.MessageId
import com.healthmetrix.recontact.commons.RequestUpdatedEvent
import com.healthmetrix.recontact.commons.UpdateType
import com.healthmetrix.recontact.jira.api.JiraApiClient
import com.healthmetrix.recontact.jira.api.JiraError
import com.healthmetrix.recontact.persistence.message.api.Message
import com.healthmetrix.recontact.persistence.message.api.MessageRepository
import com.healthmetrix.recontact.persistence.request.api.Request
import com.healthmetrix.recontact.persistence.request.api.RequestRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class RequestUpdatedEventProcessorTest {
    private val jiraApiClient: JiraApiClient = mockk()
    private val messageRepository: MessageRepository = mockk()
    private val requestRepository: RequestRepository = mockk()
    private val underTest = RequestUpdatedEventProcessor(messageRepository, requestRepository, jiraApiClient, true)

    private val requestId = UUID.randomUUID().toString()

    @Test
    fun `trigger Report for Created`() {
        val request = mockRequest(147)
        every { requestRepository.findById(requestId) } returns request
        every { messageRepository.findAllByRequestId(requestId) } returns mockMessages(147, 30, 44)
        every { jiraApiClient.updateReportField(eq(requestId), any()) } returns Ok(Unit)

        underTest.onEvent(RequestUpdatedEvent(requestId = request.id, UpdateType.REQUEST_CREATED))
        verify {
            jiraApiClient.updateReportField(
                eq(requestId),
                match {
                    it.contains(
                        """
                        Cohort size: 147
                        50.34 % of messages were delivered.
                        20.41 % of messages were read.
            
                        This report was last updated on
                        """.trimIndent(),
                    )
                },
            )
        }
    }

    @Test
    fun `trigger Report fails because of missing request`() {
        every { requestRepository.findById(requestId) } returns null

        assertThat(
            underTest.onEvent(RequestUpdatedEvent(requestId = requestId, UpdateType.REQUEST_CREATED)).unwrapError(),
        ).isInstanceOf(Throwable::class.java)
    }

    @Test
    fun `trigger Report fails because of jira api failure`() {
        val request = mockRequest(147)
        every { requestRepository.findById(requestId) } returns request
        every { messageRepository.findAllByRequestId(requestId) } returns mockMessages(147, 30, 44)
        every {
            jiraApiClient.updateReportField(
                eq(requestId),
                any(),
            )
        } returns Err(JiraError.CustomFieldNotFound("Custom field report not part of the issue ${request.id}"))

        assertThat(
            underTest.onEvent(RequestUpdatedEvent(requestId = request.id, UpdateType.REQUEST_CREATED)).unwrapError(),
        ).isInstanceOf(Throwable::class.java)
    }

    private fun mockMessages(total: Int, read: Int, delivered: Int): List<Message> {
        assertThat(total).isGreaterThanOrEqualTo(read + delivered)
        return mutableListOf<Message>().apply {
            addAll((1..read).map { mockMessage(Message.State.READ) })
            addAll((1..delivered).map { mockMessage(Message.State.DELIVERED) })
            addAll((1..total - read - delivered).map { mockMessage(Message.State.CREATED) })
        }
    }

    private fun mockMessage(state: Message.State) =
        Message(
            id = MessageId.randomUUID(),
            linkedRequest = requestId,
            createdAt = LocalDate.of(2021, 3, 5).atStartOfDay(ZoneId.of("UTC")),
            updatedAt = null,
            content = Message.Content(text = "Hello", title = "Title"),
            recipientId = UUID.randomUUID().toString(),
            state = state,
        )

    private fun mockRequest(total: Int) = Request(
        id = requestId,
        cohort = Request.Cohort(citizens = (1..total).map { UUID.randomUUID().toString() }.toSet(), name = "name"),
        active = true,
        createdAt = LocalDate.of(2021, 3, 5).atStartOfDay(ZoneId.of("UTC")),
        updatedAt = null,
        message = Request.Message(content = Request.Message.Content(title = "Title", text = "Hello!")),
    )
}
