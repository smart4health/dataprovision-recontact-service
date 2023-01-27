package com.healthmetrix.recontact.persistence.request.api

import com.healthmetrix.recontact.commons.CitizenId
import com.healthmetrix.recontact.commons.RequestId
import java.time.ZonedDateTime

data class Request(
    val id: RequestId,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime?,
    val cohort: Cohort,
    val message: Message,
    val active: Boolean,
) {
    data class Cohort(
        val citizens: Set<CitizenId>,
        val name: String,
    )

    data class Message(
        val content: Content,
    ) {
        data class Content(
            val text: String,
            val title: String,
        )
    }
}

interface RequestRepository {
    fun upsert(request: Request)
    fun findById(requestId: RequestId): Request?
    fun findAll(limit: Int): List<Request>
}
