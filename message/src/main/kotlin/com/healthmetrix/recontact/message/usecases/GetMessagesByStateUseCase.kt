package com.healthmetrix.recontact.message.usecases

import com.healthmetrix.recontact.commons.CitizenId
import com.healthmetrix.recontact.persistence.message.api.Message
import com.healthmetrix.recontact.persistence.message.api.MessageRepository
import org.springframework.stereotype.Component

@Component
class GetMessagesByStateUseCase(
    private val messageRepository: MessageRepository,
) {
    operator fun invoke(citizenId: CitizenId, state: Message.State?): List<Message> =
        messageRepository.findAllByCitizenIdAndState(citizenId, state)
}
