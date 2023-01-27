package com.healthmetrix.recontact.request.usecases

import com.healthmetrix.recontact.commons.MessageId
import com.healthmetrix.recontact.persistence.message.api.Message
import com.healthmetrix.recontact.persistence.message.api.MessageRepository
import com.healthmetrix.recontact.persistence.request.api.Request
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
class UpdateCohortMessagesUseCase(
    private val messageRepository: MessageRepository,
) {
    @Transactional
    operator fun invoke(request: Request) {
        request.cohort.citizens.forEach { citizenId ->
            if (request.active) {
                messageRepository.upsert(
                    Message(
                        id = MessageId.randomUUID(),
                        linkedRequest = request.id,
                        createdAt = ZonedDateTime.now(),
                        updatedAt = null,
                        content = Message.Content(text = request.message.content.text, title = request.message.content.title),
                        recipientId = citizenId,
                        state = Message.State.CREATED,
                    ),
                )
            } else {
                messageRepository.findByRequestIdAndCitizenId(request.id, citizenId)
                    ?.let { messageRepository.delete(it.id) }
            }
        }
    }
}
