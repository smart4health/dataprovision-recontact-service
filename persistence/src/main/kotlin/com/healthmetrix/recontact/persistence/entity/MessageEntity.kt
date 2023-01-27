package com.healthmetrix.recontact.persistence.entity

import com.healthmetrix.recontact.commons.CitizenId
import com.healthmetrix.recontact.commons.MessageId
import com.healthmetrix.recontact.commons.RequestId
import com.healthmetrix.recontact.persistence.message.api.Message
import java.sql.Timestamp
import java.time.ZoneId

data class MessageEntity(
    val id: MessageId,
    val createdAt: Timestamp,
    val updatedAt: Timestamp?,
    val linkedRequest: RequestId,
    val state: String,
    val recipientId: CitizenId,
    val contentText: String,
    val contentTitle: String,
)

fun MessageEntity.toDomain() = Message(
    id = id,
    createdAt = createdAt.toInstant().atZone(ZoneId.of("UTC")),
    updatedAt = updatedAt?.toInstant()?.atZone(ZoneId.of("UTC")),
    linkedRequest = linkedRequest,
    state = Message.State.valueOf(state),
    content = Message.Content(text = contentText, title = contentTitle),
    recipientId = recipientId,
)

fun Message.toEntity() = MessageEntity(
    id = id,
    createdAt = createdAt.toInstant().toEpochMilli().let(::Timestamp),
    updatedAt = updatedAt?.toInstant()?.toEpochMilli()?.let(::Timestamp),
    linkedRequest = linkedRequest,
    state = state.name,
    recipientId = recipientId,
    contentText = content.text,
    contentTitle = content.title,
)
