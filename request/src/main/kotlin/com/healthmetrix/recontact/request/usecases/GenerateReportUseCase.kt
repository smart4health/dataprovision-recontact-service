package com.healthmetrix.recontact.request.usecases

import com.healthmetrix.recontact.persistence.message.api.Message
import com.healthmetrix.recontact.persistence.message.api.MessageRepository
import com.healthmetrix.recontact.persistence.request.api.Request
import com.healthmetrix.recontact.persistence.request.api.RequestRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class GenerateReportUseCase(
    private val requestRepository: RequestRepository,
    private val messageRepository: MessageRepository,
) {
    @Transactional
    operator fun invoke(): Report =
        requestRepository.findAll(limit = 500)
            .partition { it.active }
            .let { (active, inactive) ->
                Report(
                    active = active.buildStats(),
                    inactive = inactive.buildStats(),
                )
            }

    private fun List<Request>.buildStats(): List<Report.RequestStats> =
        this.map { request ->
            Report.RequestStats(
                request = request,
                messageStats = messageRepository.findAllByRequestId(request.id)
                    .groupingBy { it.state }
                    .eachCount(),
            )
        }
}

data class Report(
    val active: List<RequestStats>,
    val inactive: List<RequestStats>,
) {
    data class RequestStats(
        val request: Request,
        val messageStats: Map<Message.State, Int>,
    )
}
