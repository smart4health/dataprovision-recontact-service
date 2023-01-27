package com.healthmetrix.recontact.persistence.entity

import com.healthmetrix.recontact.commons.CitizenId
import com.healthmetrix.recontact.commons.RequestId

data class CitizenCohortEntity(
    val cohortId: RequestId,
    val citizenId: CitizenId,
)
