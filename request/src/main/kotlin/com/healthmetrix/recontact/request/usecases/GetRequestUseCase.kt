package com.healthmetrix.recontact.request.usecases

import com.healthmetrix.recontact.commons.RequestId
import com.healthmetrix.recontact.persistence.request.api.Request
import com.healthmetrix.recontact.persistence.request.api.RequestRepository
import org.springframework.stereotype.Component

@Component
class GetRequestUseCase(
    private val requestRepository: RequestRepository,
) {
    operator fun invoke(requestId: RequestId): Request? = requestRepository.findById(requestId)
}
