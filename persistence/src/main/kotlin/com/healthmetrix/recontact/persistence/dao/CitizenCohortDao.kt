package com.healthmetrix.recontact.persistence.dao

import com.healthmetrix.recontact.persistence.entity.CitizenCohortEntity
import org.jdbi.v3.sqlobject.kotlin.BindKotlin
import org.jdbi.v3.sqlobject.statement.SqlBatch

interface CitizenCohortDao {

    @SqlBatch(
        """INSERT INTO citizen_cohort (
                    cohort_id,
                    citizen_id
                ) VALUES (
                    :cohortId,
                    :citizenId
                ) ON CONFLICT (cohort_id, citizen_id) DO NOTHING""",
    )
    fun insertAll(
        @BindKotlin
        citizens: Collection<CitizenCohortEntity>,
    )
}
