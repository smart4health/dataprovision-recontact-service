package com.healthmetrix.recontact.persistence.repository

import com.healthmetrix.recontact.commons.RequestId
import com.healthmetrix.recontact.persistence.dao.CitizenCohortDao
import com.healthmetrix.recontact.persistence.dao.RequestDao
import com.healthmetrix.recontact.persistence.entity.CitizenCohortEntity
import com.healthmetrix.recontact.persistence.entity.toDomain
import com.healthmetrix.recontact.persistence.entity.toEntity
import com.healthmetrix.recontact.persistence.request.api.Request
import com.healthmetrix.recontact.persistence.request.api.RequestRepository
import org.springframework.stereotype.Repository

@Repository
class JdbiRequestRepository(
    private val requestDao: RequestDao,
    private val citizenCohortDao: CitizenCohortDao,
) : RequestRepository {

    override fun upsert(request: Request) {
        requestDao.upsert(request.toEntity())
        citizenCohortDao.insertAll(request.cohort.citizens.map { CitizenCohortEntity(request.id, it) })
    }

    override fun findById(requestId: RequestId): Request? = requestDao.findById(requestId)?.toDomain()

    override fun findAll(limit: Int): List<Request> = requestDao.findAll(limit).map { it.toDomain() }
}
