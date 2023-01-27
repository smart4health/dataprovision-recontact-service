package com.healthmetrix.recontact.persistence.repository

import com.healthmetrix.recontact.commons.CitizenId
import com.healthmetrix.recontact.commons.MessageId
import com.healthmetrix.recontact.commons.RequestId
import com.healthmetrix.recontact.persistence.dao.MessageDao
import com.healthmetrix.recontact.persistence.entity.toDomain
import com.healthmetrix.recontact.persistence.entity.toEntity
import com.healthmetrix.recontact.persistence.message.api.Message
import com.healthmetrix.recontact.persistence.message.api.MessageRepository
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.ZonedDateTime

@Repository
class JdbiMessageRepository(
    private val messageDao: MessageDao,
) : MessageRepository {

    override fun upsert(message: Message) {
        messageDao.upsert(message.toEntity())
    }

    override fun delete(messageId: MessageId) {
        messageDao.delete(messageId)
    }

    override fun findById(messageId: MessageId): Message? = messageDao.findById(messageId)?.toDomain()

    override fun findByRequestIdAndCitizenId(requestId: RequestId, citizenId: CitizenId): Message? =
        messageDao.findByLinkedRequestAndRecipientId(requestId, citizenId)?.toDomain()

    override fun findAllByCitizenIdAndState(citizenId: CitizenId, state: Message.State?): List<Message> {
        return if (state != null) {
            messageDao.findAllByRecipientIdAndState(citizenId, state).map { it.toDomain() }
        } else {
            messageDao.findAllByRecipientId(citizenId).map { it.toDomain() }
        }
    }

    override fun findAllByRequestId(requestId: RequestId): List<Message> =
        messageDao.findAllByLinkedRequest(requestId).map { it.toDomain() }

    override fun findAllUpdatedAfter(time: ZonedDateTime): List<Message> =
        messageDao.findAllUpdatedAfter(time.toInstant().toEpochMilli().let(::Timestamp)).map { it.toDomain() }
}
