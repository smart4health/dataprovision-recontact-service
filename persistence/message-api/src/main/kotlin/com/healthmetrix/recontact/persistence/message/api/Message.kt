package com.healthmetrix.recontact.persistence.message.api

import com.healthmetrix.recontact.commons.CitizenId
import com.healthmetrix.recontact.commons.MessageId
import com.healthmetrix.recontact.commons.RequestId
import java.time.ZonedDateTime

data class Message(
    val id: MessageId,
    val linkedRequest: RequestId,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime?,
    val content: Content,
    val recipientId: CitizenId,
    val state: State,
) {
    data class Content(
        val text: String,
        val title: String,
    )

    enum class State {
        CREATED, DELIVERED, READ
    }
}

interface MessageRepository {
    fun upsert(message: Message)
    fun delete(messageId: MessageId)
    fun findById(messageId: MessageId): Message?
    fun findByRequestIdAndCitizenId(requestId: RequestId, citizenId: CitizenId): Message?
    fun findAllByCitizenIdAndState(citizenId: CitizenId, state: Message.State?): List<Message>
    fun findAllByRequestId(requestId: RequestId): List<Message>
    fun findAllUpdatedAfter(time: ZonedDateTime): List<Message>
}
