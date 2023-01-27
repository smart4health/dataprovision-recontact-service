package com.healthmetrix.recontact.request.usecases

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.toErrorIf
import com.github.michaelbull.result.toResultOr
import com.healthmetrix.recontact.commons.RequestId
import com.healthmetrix.recontact.persistence.request.api.RequestRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
class CancelRequestUseCase(
    private val requestRepository: RequestRepository,
    private val updateCohortMessagesUseCase: UpdateCohortMessagesUseCase,
) {
    @Transactional
    operator fun invoke(requestId: RequestId): Result<Unit, CancelRequestException> = binding {
        val request = requestRepository.findById(requestId)
            .toResultOr { CancelRequestException.NotFound }
            .toErrorIf(
                predicate = { !it.active },
                transform = { CancelRequestException.AlreadyCancelled },
            )
            .bind()
            .copy(active = false, updatedAt = ZonedDateTime.now())

        requestRepository.upsert(request)
        updateCohortMessagesUseCase(request)
    }
}

sealed class CancelRequestException {
    object AlreadyCancelled : CancelRequestException()
    object NotFound : CancelRequestException()
}
