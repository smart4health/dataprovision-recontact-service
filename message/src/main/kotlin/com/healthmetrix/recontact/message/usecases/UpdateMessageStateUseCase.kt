package com.healthmetrix.recontact.message.usecases

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.toErrorIf
import com.github.michaelbull.result.toResultOr
import com.healthmetrix.recontact.commons.CitizenId
import com.healthmetrix.recontact.commons.MessageId
import com.healthmetrix.recontact.persistence.message.api.Message
import com.healthmetrix.recontact.persistence.message.api.MessageRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

typealias StateChanged = Boolean

@Component
class UpdateMessageStateUseCase(
    private val messageRepository: MessageRepository,
) {
    @Transactional
    operator fun invoke(
        messageId: MessageId,
        citizenId: CitizenId,
        newState: Message.State,
    ): Result<Pair<Message, StateChanged>, UpdateMessageException> = binding {
        val message = messageRepository.findById(messageId)
            .toResultOr { UpdateMessageException.NotFound }
            .toErrorIf(
                predicate = { it.recipientId != citizenId },
                transform = { UpdateMessageException.NotAllowed },
            )
            .bind()

        if (message.state.canTransitionTo(newState)) {
            message.copy(state = newState, updatedAt = ZonedDateTime.now()).also {
                messageRepository.upsert(it)
            } to true
        } else {
            message to false
        }
    }
}

private fun Message.State.canTransitionTo(newState: Message.State): Boolean = when (this) {
    Message.State.CREATED -> newState == Message.State.DELIVERED || newState == Message.State.READ
    Message.State.DELIVERED -> newState == Message.State.READ
    Message.State.READ -> false
}

sealed class UpdateMessageException {
    object NotFound : UpdateMessageException()
    object NotAllowed : UpdateMessageException()
}
