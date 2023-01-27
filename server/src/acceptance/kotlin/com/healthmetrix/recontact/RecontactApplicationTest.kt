package com.healthmetrix.recontact

import com.healthmetrix.recontact.commons.CitizenId
import com.healthmetrix.recontact.commons.MessageId
import com.healthmetrix.recontact.commons.RequestId
import com.healthmetrix.recontact.commons.test.array
import com.healthmetrix.recontact.commons.test.json
import com.healthmetrix.recontact.persistence.message.api.Message
import com.healthmetrix.recontact.persistence.message.api.MessageRepository
import com.healthmetrix.recontact.persistence.request.api.Request
import com.healthmetrix.recontact.persistence.request.api.RequestRepository
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.hasSize
import org.hamcrest.core.Is.`is`
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.ZonedDateTime
import java.util.UUID

private const val HEADER_NAME = "X-Recontact-Citizen-Id"

@Suppress("FunctionName")
@SpringBootTest(
    classes = [RecontactApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@AutoConfigureMockMvc
class RecontactApplicationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val messageRepository: MessageRepository,
    private val requestRepository: RequestRepository,
) {

    @Nested
    inner class RequestController {
        @Test
        fun `GET requests`() {
            mockMvc.get("/v1/request/${UUID.randomUUID()}")
                .andExpect { status { isNotFound() } }
        }

        @Test
        fun `add new requests and cancel`() {
            val requestId = UUID.randomUUID().toString()

            mockMvc.post("/v1/requests") {
                contentType = MediaType.APPLICATION_JSON
                content = json {
                    "id" to requestId
                    "message" to "Hello my friend!"
                    "title" to "title"
                }
            }
                .andExpect { status { isCreated() } }
                .andExpect { jsonPath("$.requestId", `is`(requestId)) }

            mockMvc.get("/v1/request/$requestId")
                .andExpect { status { isOk() } }
                .andExpect { jsonPath("$.active", `is`(true)) }

            assertThat(messageRepository.findAllByRequestId(requestId).filter { it.state == Message.State.CREATED })
                .hasSize(20)

            // Cancel the Request, Message will be removed
            mockMvc.delete("/v1/request/$requestId")
                .andExpect { status { isOk() } }

            mockMvc.get("/v1/request/$requestId")
                .andExpect { status { isOk() } }
                .andExpect { jsonPath("$.active", `is`(false)) }

            assertThat(messageRepository.findAllByRequestId(requestId).filter { it.state == Message.State.CREATED })
                .hasSize(0)
        }

        @Test
        fun `testing the jdbi repo`() {
            Assertions.assertThat(requestRepository.findById("notexistenttest")).isNull()
            val firstRequest = mockRequest(
                UUID.randomUUID().toString(),
                setOf(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString()),
            )
            val secondRequest = mockRequest(UUID.randomUUID().toString(), setOf(UUID.randomUUID().toString()))

            requestRepository.upsert(firstRequest)
            Assertions.assertThat(requestRepository.findById(firstRequest.id)).isNotNull
            Assertions.assertThat(requestRepository.findById(firstRequest.id)?.active).isTrue

            requestRepository.upsert(secondRequest)
            Assertions.assertThat(requestRepository.findById(firstRequest.id)).isNotNull
            requestRepository.findAll(1).also {
                Assertions.assertThat(it).hasSize(1)
            }

            // Test third request with same cohort as first request
            val thirdRequest = firstRequest.copy(id = UUID.randomUUID().toString())
            requestRepository.upsert(thirdRequest)
            Assertions.assertThat(requestRepository.findById(thirdRequest.id)).isNotNull

            requestRepository.upsert(firstRequest.copy(active = false))
            Assertions.assertThat(requestRepository.findById(firstRequest.id)?.active).isFalse
        }

        private fun mockRequest(requestId: String, citizenIds: Set<CitizenId>) = Request(
            id = requestId,
            cohort = Request.Cohort(citizens = citizenIds, name = "name"),
            active = true,
            createdAt = ZonedDateTime.now(),
            updatedAt = null,
            message = Request.Message(content = Request.Message.Content(title = "Title", text = "Hello!")),
        )
    }

    @Nested
    inner class MessageController {
        @ParameterizedTest
        @NullSource
        @ValueSource(strings = ["created", "read", "delivered"])
        fun `GET messages with implicit delivered state`(state: String?) {
            val citizenId = UUID.randomUUID().toString()
            val messageSizes = mapOf("read" to 5, "delivered" to 4, "created" to 3, null to 12)
            val readMessages = generateSequence(UUID::randomUUID).take(5).toList()
                .map { mockMessage(it, citizenId, state = Message.State.READ) }
            val deliveredMessages = generateSequence(UUID::randomUUID).take(4).toList()
                .map { mockMessage(it, citizenId, state = Message.State.DELIVERED) }
            val createdMessages = generateSequence(UUID::randomUUID).take(3).toList()
                .map { mockMessage(it, citizenId, state = Message.State.CREATED) }
            readMessages.forEach { messageRepository.upsert(it) }
            deliveredMessages.forEach { messageRepository.upsert(it) }
            createdMessages.forEach { messageRepository.upsert(it) }

            mockMvc.get("/v1/messages") {
                headers { add(HEADER_NAME, citizenId) }
                state?.let { param("state", state) }
            }.andExpect { status { isOk() } }
                .andExpect {
                    jsonPath(
                        "$.messages",
                        hasSize<Array<Any>>(messageSizes[state]!!),
                    )
                }

            val messageToStates = messageRepository.findAllByCitizenIdAndState(citizenId, null)
                .groupBy { message -> message.state }

            if (state == "read" || state == "delivered") {
                // No message states should be updated if just pulling READ and DELIVERED
                assertThat(messageToStates[Message.State.READ]!!.size).isEqualTo(5)
                assertThat(messageToStates[Message.State.DELIVERED]!!.size).isEqualTo(4)
                assertThat(messageToStates[Message.State.CREATED]!!.size).isEqualTo(3)
            } else {
                // created ones are marked as delivered
                assertThat(messageToStates[Message.State.READ]!!.size).isEqualTo(5)
                assertThat(messageToStates[Message.State.DELIVERED]!!.size).isEqualTo(7)
                assertThat(messageToStates[Message.State.CREATED]).isNull()
            }
        }

        @Test
        fun `update multiple messages`() {
            val citizenId = UUID.randomUUID().toString()
            val readMessages = generateSequence(UUID::randomUUID).take(5).toList()
                .map { mockMessage(it, citizenId, state = Message.State.READ) }
            val deliveredMessages = generateSequence(UUID::randomUUID).take(4).toList()
                .map { mockMessage(it, citizenId, state = Message.State.DELIVERED) }
            val createdMessages = generateSequence(UUID::randomUUID).take(3).toList()
                .map { mockMessage(it, citizenId, state = Message.State.CREATED) }
            readMessages.forEach { messageRepository.upsert(it) }
            deliveredMessages.forEach { messageRepository.upsert(it) }
            createdMessages.forEach { messageRepository.upsert(it) }

            mockMvc.post("/v1/messages") {
                headers { add(HEADER_NAME, citizenId) }
                contentType = MediaType.APPLICATION_JSON
                content = json {
                    "toUpdate" to array {
                        json {
                            "action" to "READ"
                            "messageId" to deliveredMessages[0].id
                        }
                        json {
                            "action" to "READ"
                            "messageId" to deliveredMessages[1].id
                        }
                        json {
                            "action" to "READ"
                            "messageId" to deliveredMessages[2].id
                        }
                        json {
                            "action" to "READ"
                            "messageId" to readMessages[0].id
                        }
                    }
                }
            }.andExpect { status { isOk() } }
                .andExpect {
                    jsonPath(
                        "$.messages",
                        hasSize<Array<Any>>(4),
                    )
                }

            messageRepository.findAllByCitizenIdAndState(citizenId, null)
                .groupBy { message -> message.state }
                .also {
                    assertThat(it[Message.State.READ]!!.size).isEqualTo(8)
                    assertThat(it[Message.State.DELIVERED]!!.size).isEqualTo(1)
                    assertThat(it[Message.State.CREATED]!!.size).isEqualTo(3)
                }
        }

        @Test
        fun `update message that has doesnt exist`() {
            mockMvc.post("/v1/messages") {
                headers { add(HEADER_NAME, UUID.randomUUID().toString()) }
                contentType = MediaType.APPLICATION_JSON
                content = json {
                    "toUpdate" to array {
                        json {
                            "action" to "READ"
                            "messageId" to MessageId.randomUUID()
                        }
                    }
                }
            }.andExpect { status { isOk() } }
                .andExpect {
                    jsonPath(
                        "$.messages",
                        hasSize<Array<Any>>(0),
                    )
                }
                .andExpect {
                    jsonPath(
                        "$.errors",
                        hasSize<Array<Any>>(1),
                    )
                }
        }

        @Test
        fun `update message for foreign citizenId must fail`() {
            val messageId = MessageId.randomUUID().also {
                messageRepository.upsert(mockMessage(messageId = it, recipientId = "Bob"))
            }
            mockMvc.post("/v1/messages") {
                headers { add(HEADER_NAME, "Alice") }
                contentType = MediaType.APPLICATION_JSON
                content = json {
                    "toUpdate" to array {
                        json {
                            "action" to "READ"
                            "messageId" to messageId
                        }
                    }
                }
            }.andExpect { status { isOk() } }
                .andExpect {
                    jsonPath(
                        "$.messages",
                        hasSize<Array<Any>>(0),
                    )
                }
                .andExpect {
                    jsonPath(
                        "$.errors",
                        hasSize<Array<Any>>(1),
                    )
                }
        }

        @Test
        fun `GET messages without id header fails`() {
            mockMvc.get("/v1/messages") {
            }.andExpect { status { is4xxClientError() } }
        }

        @Test
        fun `set message to delivered and read`() {
            val messageId = MessageId.randomUUID()
            val citizenId = UUID.randomUUID().toString()
            messageRepository.upsert(mockMessage(messageId, citizenId))

            mockMvc.get("/v1/message/$messageId") {
                headers { add(HEADER_NAME, citizenId) }
                contentType = MediaType.APPLICATION_JSON
            }
                .andExpect { status { isOk() } }
                .andExpect { jsonPath("$.state", `is`("DELIVERED")) }

            assertThat(messageRepository.findById(messageId)!!.state.name).isEqualTo("DELIVERED")

            mockMvc.post("/v1/message/$messageId") {
                headers { add(HEADER_NAME, citizenId) }
                contentType = MediaType.APPLICATION_JSON
                content = json { "action" to "read" }
            }
                .andExpect { status { isOk() } }
                .andExpect { jsonPath("$.state", `is`("READ")) }

            assertThat(messageRepository.findById(messageId)!!.state.name).isEqualTo("READ")
        }

        @Test
        fun `set message to read without id header fails`() {
            mockMvc.post("/v1/message/${MessageId.randomUUID()}") {
                contentType = MediaType.APPLICATION_JSON
                content = json { "action" to "read" }
            }.andExpect { status { is4xxClientError() } }
        }

        @Test
        fun `testing the jdbi repo`() {
            val messageId = MessageId.randomUUID()
            val citizenId = UUID.randomUUID().toString()
            val requestId = UUID.randomUUID().toString()

            messageRepository.upsert(mockMessage(messageId, citizenId, requestId))
            assertThat(messageRepository.findById(messageId)).isNotNull
            assertThat(messageRepository.findAllByRequestId(requestId)).hasSize(1)
            assertThat(messageRepository.findAllByCitizenIdAndState(citizenId, null)).hasSize(1)
            assertThat(
                messageRepository.findAllByCitizenIdAndState(
                    citizenId,
                    Message.State.CREATED,
                ),
            ).hasSize(1)
            assertThat(messageRepository.findByRequestIdAndCitizenId(requestId, citizenId)).isNotNull
            messageRepository.delete(messageId)
            assertThat(messageRepository.findById(messageId)).isNull()
        }

        @Test
        fun `get message and update message for other citizen should not be possible`() {
            val messageId = MessageId.randomUUID()
            messageRepository.upsert(mockMessage(messageId, UUID.randomUUID().toString()))

            mockMvc.get("/v1/message/$messageId") {
                headers { add(HEADER_NAME, "a different citizen") }
                contentType = MediaType.APPLICATION_JSON
            }
                .andExpect { status { isUnauthorized() } }

            mockMvc.post("/v1/message/$messageId") {
                headers { add(HEADER_NAME, "a different citizen") }
                contentType = MediaType.APPLICATION_JSON
                content = json { "action" to "read" }
            }
                .andExpect { status { isUnauthorized() } }

            assertThat(messageRepository.findById(messageId)!!.state.name).isEqualTo("CREATED")
        }

        private fun mockMessage(
            messageId: MessageId,
            recipientId: CitizenId,
            requestId: RequestId = UUID.randomUUID().toString(),
            state: Message.State = Message.State.CREATED,
        ) = Message(
            id = messageId,
            linkedRequest = requestId,
            createdAt = ZonedDateTime.now(),
            updatedAt = null,
            content = Message.Content(text = "Hello", title = "Title"),
            recipientId = recipientId,
            state = state,
        )
    }
}
