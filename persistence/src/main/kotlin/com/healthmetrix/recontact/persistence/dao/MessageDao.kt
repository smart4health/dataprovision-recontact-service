package com.healthmetrix.recontact.persistence.dao

import com.healthmetrix.recontact.commons.CitizenId
import com.healthmetrix.recontact.commons.MessageId
import com.healthmetrix.recontact.commons.RequestId
import com.healthmetrix.recontact.persistence.entity.MessageEntity
import com.healthmetrix.recontact.persistence.message.api.Message
import org.jdbi.v3.sqlobject.kotlin.BindKotlin
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import java.sql.Timestamp

interface MessageDao {

    @SqlQuery("SELECT * FROM message WHERE linked_request = :linkedRequest")
    fun findAllByLinkedRequest(
        linkedRequest: RequestId,
    ): List<MessageEntity>

    @SqlQuery("SELECT * FROM message WHERE updated_at > :timestamp")
    fun findAllUpdatedAfter(
        timestamp: Timestamp,
    ): List<MessageEntity>

    @SqlUpdate(
        """INSERT INTO message (
                    id,
                    created_at,
                    updated_at,
                    linked_request,
                    state,
                    recipient_id,
                    content_text,
                    content_title
                ) VALUES (
                    :id,
                    :createdAt,
                    :updatedAt,
                    :linkedRequest,
                    :state,
                    :recipientId,
                    :contentText,
                    :contentTitle
                ) ON CONFLICT (id) DO UPDATE SET
                    created_at = :createdAt,
                    updated_at = :updatedAt,
                    linked_request = :linkedRequest,
                    state = :state,
                    recipient_id = :recipientId,
                    content_text = :contentText,
                    content_title = :contentTitle
                """,
    )
    fun upsert(
        @BindKotlin
        messageEntity: MessageEntity,
    )

    @SqlQuery("SELECT * FROM message WHERE id = :id")
    fun findById(
        id: MessageId,
    ): MessageEntity?

    @SqlQuery("SELECT * FROM message WHERE recipient_id = :recipientId")
    fun findAllByRecipientId(
        recipientId: CitizenId,
    ): List<MessageEntity>

    @SqlQuery("SELECT * FROM message WHERE recipient_id = :recipientId AND state = :state")
    fun findAllByRecipientIdAndState(
        recipientId: CitizenId,
        state: Message.State?,
    ): List<MessageEntity>

    @SqlQuery("SELECT * FROM message WHERE linked_request = :linkedRequest AND recipient_id = :recipientId")
    fun findByLinkedRequestAndRecipientId(
        linkedRequest: RequestId,
        recipientId: CitizenId,
    ): MessageEntity?

    @SqlUpdate("DELETE FROM message WHERE id = :id")
    fun delete(
        id: MessageId,
    )
}
