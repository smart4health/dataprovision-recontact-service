package com.healthmetrix.recontact.persistence.dao

import com.healthmetrix.recontact.commons.RequestId
import com.healthmetrix.recontact.persistence.entity.CitizenCohortEntity
import com.healthmetrix.recontact.persistence.entity.RequestEntity
import org.jdbi.v3.core.result.LinkedHashMapRowReducer
import org.jdbi.v3.core.result.RowView
import org.jdbi.v3.sqlobject.kotlin.BindKotlin
import org.jdbi.v3.sqlobject.kotlin.RegisterKotlinMapper
import org.jdbi.v3.sqlobject.kotlin.RegisterKotlinMappers
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.jdbi.v3.sqlobject.statement.UseRowReducer

const val REQUEST_PREFIX = "r"
const val COHORT_PREFIX = "c"
const val REQUEST_COLUMNS = """
    $REQUEST_PREFIX.id ${REQUEST_PREFIX}_id,
    $REQUEST_PREFIX.created_at ${REQUEST_PREFIX}_created_at,
    $REQUEST_PREFIX.updated_at ${REQUEST_PREFIX}_updated_at,
    $REQUEST_PREFIX.cohort_name ${REQUEST_PREFIX}_cohort_name,
    $REQUEST_PREFIX.message_content_text ${REQUEST_PREFIX}_message_content_text,
    $REQUEST_PREFIX.message_content_title ${REQUEST_PREFIX}_message_content_title,
    $REQUEST_PREFIX.active ${REQUEST_PREFIX}_active
"""
const val COHORT_COLUMNS = """
    $COHORT_PREFIX.cohort_id ${COHORT_PREFIX}_cohort_id,
    $COHORT_PREFIX.citizen_id ${COHORT_PREFIX}_citizen_id
"""

interface RequestDao {

    @SqlUpdate(
        """INSERT INTO request (
                    id,
                    created_at,
                    updated_at,
                    cohort_name,
                    message_content_text,
                    message_content_title,
                    active
                ) VALUES (
                    :id,
                    :createdAt,
                    :updatedAt,
                    :cohortName,
                    :messageContentText,
                    :messageContentTitle,
                    :active
                ) ON CONFLICT (id) DO UPDATE SET
                    created_at = :createdAt,
                    updated_at = :updatedAt,
                    cohort_name = :cohortName,
                    message_content_text = :messageContentText,
                    message_content_title = :messageContentTitle,
                    active = :active
                """,
    )
    fun upsert(
        @BindKotlin
        requestEntity: RequestEntity,
    )

    @SqlQuery(
        """SELECT $REQUEST_COLUMNS, $COHORT_COLUMNS
                FROM request $REQUEST_PREFIX
                LEFT JOIN citizen_cohort $COHORT_PREFIX 
                ON $REQUEST_PREFIX.id = $COHORT_PREFIX.cohort_id
                WHERE $REQUEST_PREFIX.id = :id
                """,
    )
    @RegisterKotlinMappers(
        RegisterKotlinMapper(value = RequestEntity::class, prefix = REQUEST_PREFIX),
        RegisterKotlinMapper(value = CitizenCohortEntity::class, prefix = COHORT_PREFIX),
    )
    @UseRowReducer(RequestRowReducer::class)
    fun findById(
        id: RequestId,
    ): RequestEntity?

    @SqlQuery(
        """SELECT $REQUEST_COLUMNS, $COHORT_COLUMNS
                FROM request $REQUEST_PREFIX
                LEFT JOIN citizen_cohort $COHORT_PREFIX 
                ON $REQUEST_PREFIX.id = $COHORT_PREFIX.cohort_id
                ORDER BY $REQUEST_PREFIX.created_at DESC LIMIT :limit
                """,
    )
    @RegisterKotlinMappers(
        RegisterKotlinMapper(value = RequestEntity::class, prefix = REQUEST_PREFIX),
        RegisterKotlinMapper(value = CitizenCohortEntity::class, prefix = COHORT_PREFIX),
    )
    @UseRowReducer(RequestRowReducer::class)
    fun findAll(limit: Int): List<RequestEntity>
}

class RequestRowReducer : LinkedHashMapRowReducer<String, RequestEntity> {
    override fun accumulate(map: MutableMap<String, RequestEntity>, rowView: RowView) {
        val request: RequestEntity = map.computeIfAbsent(
            rowView.getColumn(
                "${REQUEST_PREFIX}_id",
                String::class.java,
            ),
        ) { rowView.getRow(RequestEntity::class.java) }
        request.cohortCitizens.add(rowView.getColumn("${COHORT_PREFIX}_citizen_id", String::class.java))
    }
}
