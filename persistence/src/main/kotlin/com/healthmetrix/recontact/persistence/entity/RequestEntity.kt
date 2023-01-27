package com.healthmetrix.recontact.persistence.entity

import com.healthmetrix.recontact.commons.CitizenId
import com.healthmetrix.recontact.persistence.request.api.Request
import java.sql.Timestamp
import java.time.ZoneId

data class RequestEntity(
    val id: String,
    val createdAt: Timestamp,
    val updatedAt: Timestamp?,
    val cohortName: String,
    val messageContentText: String,
    val messageContentTitle: String,
    val active: Boolean,
    // accumulated from CitizenCohortEntity in RequestDao
    val cohortCitizens: MutableSet<CitizenId> = mutableSetOf(),
)

fun RequestEntity.toDomain() = Request(
    id = id,
    createdAt = createdAt.toInstant().atZone(ZoneId.of("UTC")),
    updatedAt = updatedAt?.toInstant()?.atZone(ZoneId.of("UTC")),
    cohort = Request.Cohort(citizens = cohortCitizens, name = cohortName),
    message = Request.Message(
        content = Request.Message.Content(
            text = messageContentText,
            title = messageContentTitle,
        ),
    ),
    active = active,
)

fun Request.toEntity() = RequestEntity(
    id = id,
    createdAt = createdAt.toInstant().toEpochMilli().let(::Timestamp),
    updatedAt = updatedAt?.toInstant()?.toEpochMilli()?.let(::Timestamp),
    cohortName = cohort.name,
    messageContentText = message.content.text,
    messageContentTitle = message.content.title,
    active = active,
    cohortCitizens = cohort.citizens.toMutableSet(),
)
