package com.healthmetrix.recontact.message.controller

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.getAll
import com.github.michaelbull.result.getAllErrors
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.merge
import com.github.michaelbull.result.onSuccess
import com.github.michaelbull.result.toResultOr
import com.healthmetrix.recontact.commons.ApiResponse
import com.healthmetrix.recontact.commons.CitizenId
import com.healthmetrix.recontact.commons.MessageId
import com.healthmetrix.recontact.commons.RequestId
import com.healthmetrix.recontact.commons.RequestUpdatedEvent
import com.healthmetrix.recontact.commons.UpdateType
import com.healthmetrix.recontact.commons.asEntity
import com.healthmetrix.recontact.message.usecases.GetMessagesByStateUseCase
import com.healthmetrix.recontact.message.usecases.UpdateMessageException
import com.healthmetrix.recontact.message.usecases.UpdateMessageStateUseCase
import com.healthmetrix.recontact.persistence.message.api.Message
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private const val CITIZEN_ID_HEADER = "X-Recontact-Citizen-Id"

@RestController
class MessageController(
    private val updateMessageStateUseCase: UpdateMessageStateUseCase,
    private val getMessagesByStateUseCase: GetMessagesByStateUseCase,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {

    @GetMapping("/v1/messages")
    fun getMessagesAndUpdateState(
        @RequestHeader(CITIZEN_ID_HEADER)
        citizenId: CitizenId,
        @RequestParam("state", required = false)
        stateParam: String?,
    ): ResponseEntity<ListMessagesResponse> = binding<ListMessagesResponse, ListMessagesResponse> {
        val state = stateParam?.let {
            it.toMessageState()
                .toResultOr {
                    ListMessagesResponse.InvalidState
                }.bind()
        }
        val updateResults = getMessagesByStateUseCase(citizenId, state)
            .map { message ->
                updateMessageStateUseCase.invoke(
                    messageId = message.id,
                    citizenId = message.recipientId,
                    newState = Message.State.DELIVERED,
                )
            }
            .getAll()
        updateResults.forEach { (message, stateChanged) -> publishUpdateEvent(message.linkedRequest, stateChanged) }
        updateResults.map { it.first }
            .let { ListMessagesResponse.Success(it) }
    }.merge().asEntity()

    @GetMapping("/v1/message/{messageId}")
    fun getMessagesAndUpdateStateToDelivered(
        @RequestHeader(CITIZEN_ID_HEADER)
        citizenId: CitizenId,
        @PathVariable("messageId")
        messageId: MessageId,
    ): ResponseEntity<GetMessageResponse> = binding<GetMessageResponse, GetMessageResponse> {
        updateMessageStateUseCase(messageId, citizenId, Message.State.DELIVERED)
            .mapError { error ->
                when (error) {
                    is UpdateMessageException.NotFound -> GetMessageResponse.NotFound
                    is UpdateMessageException.NotAllowed -> GetMessageResponse.NotAllowed
                }
            }.onSuccess { (message, stateChanged) -> publishUpdateEvent(message.linkedRequest, stateChanged) }
            .map { GetMessageResponse.Found(it.first) }
            .bind()
    }.merge().asEntity()

    @PostMapping("/v1/message/{messageId}")
    fun updateMessage(
        @PathVariable("messageId")
        messageId: MessageId,
        @RequestHeader(CITIZEN_ID_HEADER)
        citizenId: CitizenId,
        @RequestBody
        updateMessageData: UpdateMessageData,
    ): ResponseEntity<UpdateMessageResponse> = binding<UpdateMessageResponse, UpdateMessageResponse> {
        val action = updateMessageData.action.toAction()
            .toResultOr {
                UpdateMessageResponse.InvalidState
            }.bind()
        updateMessageStateUseCase(messageId, citizenId, action.toState())
            .mapError { error ->
                when (error) {
                    is UpdateMessageException.NotFound -> UpdateMessageResponse.NotFound
                    is UpdateMessageException.NotAllowed -> UpdateMessageResponse.NotAllowed
                }
            }.onSuccess { (message, updated) -> publishUpdateEvent(message.linkedRequest, updated) }
            .map { UpdateMessageResponse.Updated(it.first) }
            .bind()
    }.merge().asEntity()

    @PostMapping("/v1/messages")
    fun updateMultipleMessages(
        @RequestHeader(CITIZEN_ID_HEADER)
        citizenId: CitizenId,
        @RequestBody
        messagesData: UpdateMultipleMessagesData,
    ): ResponseEntity<UpdateMultipleMessagesResponse> =
        binding<UpdateMultipleMessagesResponse, UpdateMultipleMessagesResponse> {
            val (success, errors) = messagesData.toUpdate
                .map { item ->
                    updateMessageStateUseCase.invoke(
                        messageId = item.messageId,
                        citizenId = citizenId,
                        newState = item.action.toMessageState()
                            .toResultOr { UpdateMultipleMessagesResponse.InvalidState }
                            .bind(),
                    )
                }
                .let { it.getAll() to it.getAllErrors() }

            val errorIds = if (errors.isNotEmpty()) {
                val successIds = success.map { it.first.id }
                messagesData.toUpdate.map { it.messageId }.filter { id -> !successIds.contains(id) }
            } else {
                emptyList()
            }

            success.forEach { (message, updated) -> publishUpdateEvent(message.linkedRequest, updated) }
            success.map { it.first }
                .let { UpdateMultipleMessagesResponse.Updated(it, errorIds) }
        }.merge().asEntity()

    private fun publishUpdateEvent(linkedRequest: RequestId, stateChange: Boolean) {
        if (stateChange) {
            applicationEventPublisher.publishEvent(
                RequestUpdatedEvent(
                    requestId = linkedRequest,
                    updateType = UpdateType.MESSAGE_STATE_CHANGED,
                ),
            )
        }
    }

    sealed class ListMessagesResponse(override val status: HttpStatus) : ApiResponse {
        data class Success(
            val messages: List<Message>,
        ) : ListMessagesResponse(HttpStatus.OK)

        object InvalidState : ListMessagesResponse(HttpStatus.BAD_REQUEST) {
            @Suppress("MayBeConstant", "unused")
            val message = "State must be one of ${Message.State.values().joinToString(", ", "[", "]")}."
        }
    }

    sealed class GetMessageResponse : ApiResponse {
        data class Found(
            @JsonUnwrapped
            val message: Message,
        ) : GetMessageResponse()

        object NotFound : GetMessageResponse() {
            override val status = HttpStatus.NOT_FOUND
            override val hasBody: Boolean = false
        }

        object NotAllowed : GetMessageResponse() {
            override val status = HttpStatus.UNAUTHORIZED
            override val hasBody: Boolean = false
        }
    }

    sealed class UpdateMessageResponse(override val status: HttpStatus) : ApiResponse {
        data class Updated(
            @JsonUnwrapped
            val message: Message,
        ) : UpdateMessageResponse(HttpStatus.OK)

        object InvalidState : UpdateMessageResponse(HttpStatus.BAD_REQUEST) {
            @Suppress("MayBeConstant", "unused")
            val message = "Action must be one of ${UpdateAction.values().joinToString(", ", "[", "]")}."
        }

        object NotFound : UpdateMessageResponse(HttpStatus.NOT_FOUND) {
            override val hasBody = false
        }

        object NotAllowed : UpdateMessageResponse(HttpStatus.UNAUTHORIZED) {
            override val hasBody = false
        }
    }

    sealed class UpdateMultipleMessagesResponse(override val status: HttpStatus) : ApiResponse {
        data class Updated(
            val messages: List<Message>,
            val errors: List<MessageId>,
        ) : UpdateMultipleMessagesResponse(HttpStatus.OK)

        object InvalidState : UpdateMultipleMessagesResponse(HttpStatus.BAD_REQUEST) {
            @Suppress("MayBeConstant", "unused")
            val message = "Action must be one of ${UpdateAction.values().joinToString(", ", "[", "]")}."
        }
    }
}

data class UpdateMultipleMessagesData(
    val toUpdate: List<MessageItem>,
) {
    data class MessageItem(val messageId: MessageId, val action: String)
}

data class UpdateMessageData(
    val action: String,
)

enum class UpdateAction {
    READ,
    ;

    fun toState(): Message.State = when (this) {
        READ -> Message.State.READ
    }
}

fun String.toMessageState(): Message.State? =
    Message.State.values().firstOrNull { it.name.equals(other = this, ignoreCase = true) }

fun String.toAction(): UpdateAction =
    UpdateAction.values().first { it.name.equals(other = this, ignoreCase = true) }
