package com.healthmetrix.recontact.request.controller

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.merge
import com.github.michaelbull.result.onSuccess
import com.github.michaelbull.result.toResultOr
import com.healthmetrix.recontact.commons.ApiResponse
import com.healthmetrix.recontact.commons.CohortInfoChanged
import com.healthmetrix.recontact.commons.RequestId
import com.healthmetrix.recontact.commons.RequestUpdatedEvent
import com.healthmetrix.recontact.commons.UpdateType
import com.healthmetrix.recontact.commons.asEntity
import com.healthmetrix.recontact.persistence.request.api.Request
import com.healthmetrix.recontact.request.usecases.CancelRequestException
import com.healthmetrix.recontact.request.usecases.CancelRequestUseCase
import com.healthmetrix.recontact.request.usecases.CreateRequestFromRemoteError
import com.healthmetrix.recontact.request.usecases.CreateRequestFromRemoteUseCase
import com.healthmetrix.recontact.request.usecases.GetRequestUseCase
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class RequestController(
    private val createRequestFromRemoteUseCase: CreateRequestFromRemoteUseCase,
    private val cancelRequestUseCase: CancelRequestUseCase,
    private val getRequestUseCase: GetRequestUseCase,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {

    @PostMapping("/v1/requests")
    fun createRequest(
        @RequestBody
        createRequestData: CreateRequestData,
    ): ResponseEntity<CreateRequestResponse> =
        createRequestFromRemoteUseCase(createRequestData)
            .mapError { error ->
                when (error) {
                    CreateRequestFromRemoteError.Duplicate -> CreateRequestResponse.ConflictingDuplicate
                    CreateRequestFromRemoteError.JiraCohortInfo -> CreateRequestResponse.JiraApiError
                }
            }.map { CreateRequestResponse.RequestCreated(it.id) }
            .onSuccess {
                applicationEventPublisher.publishEvent(
                    RequestUpdatedEvent(
                        requestId = createRequestData.id,
                        updateType = UpdateType.REQUEST_CREATED,
                    ),
                )
                applicationEventPublisher.publishEvent(CohortInfoChanged(issueId = createRequestData.id))
            }
            .merge().asEntity()

    @GetMapping("/v1/request/{requestId}")
    fun getRequest(
        @PathVariable("requestId")
        requestId: RequestId,
    ): ResponseEntity<GetRequestResponse> =
        getRequestUseCase(requestId)
            .toResultOr { GetRequestResponse.GetRequestNotFound }
            .map { GetRequestResponse.Found(it) }
            .merge().asEntity()

    @DeleteMapping("/v1/request/{requestId}")
    fun cancelRequest(
        @PathVariable("requestId")
        requestId: RequestId,
    ): ResponseEntity<CancelRequestResponse> =
        cancelRequestUseCase(requestId)
            .mapError { error ->
                when (error) {
                    is CancelRequestException.NotFound -> CancelRequestResponse.NotFound
                    is CancelRequestException.AlreadyCancelled -> CancelRequestResponse.AlreadyCancelled
                }
            }.map { CancelRequestResponse.Cancelled }
            .onSuccess {
                applicationEventPublisher.publishEvent(
                    RequestUpdatedEvent(
                        requestId = requestId,
                        updateType = UpdateType.REQUEST_CANCELLED,
                    ),
                )
            }
            .merge().asEntity()

    sealed class CreateRequestResponse : ApiResponse {

        data class RequestCreated(
            val requestId: RequestId,
        ) : CreateRequestResponse() {
            override val status = HttpStatus.CREATED
        }

        object ConflictingDuplicate : CreateRequestResponse() {
            override val status = HttpStatus.CONFLICT

            @Suppress("MayBeConstant", "unused")
            val message = "Request with this Ticket ID already created."
        }

        object JiraApiError : CreateRequestResponse() {
            override val status = HttpStatus.INTERNAL_SERVER_ERROR

            @Suppress("MayBeConstant", "unused")
            val message = "Connection with Api failed."
        }
    }

    sealed class GetRequestResponse : ApiResponse {
        data class Found(
            @JsonUnwrapped
            val request: Request,
        ) : GetRequestResponse()

        object GetRequestNotFound : GetRequestResponse() {
            override val status = HttpStatus.NOT_FOUND
            override val hasBody: Boolean = false
        }
    }

    sealed class CancelRequestResponse(
        override val status: HttpStatus,
        override val hasBody: Boolean,
    ) : ApiResponse {
        object Cancelled : CancelRequestResponse(HttpStatus.OK, false)
        object NotFound : CancelRequestResponse(HttpStatus.NOT_FOUND, false)
        object AlreadyCancelled : CancelRequestResponse(HttpStatus.OK, true) {
            @Suppress("MayBeConstant", "unused")
            val message = "Request is cancelled and cannot be cancelled again."
        }
    }
}

data class CreateRequestData(
    val id: RequestId,
    val message: String,
    val title: String,
)
