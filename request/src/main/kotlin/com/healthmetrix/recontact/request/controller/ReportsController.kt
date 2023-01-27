package com.healthmetrix.recontact.request.controller

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.healthmetrix.recontact.commons.ApiResponse
import com.healthmetrix.recontact.commons.asEntity
import com.healthmetrix.recontact.request.usecases.GenerateReportUseCase
import com.healthmetrix.recontact.request.usecases.Report
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Generate reports, only used for internal purposes for now.
 */
@RestController
class ReportsController(
    private val generateReportUseCase: GenerateReportUseCase,
) {

    @GetMapping("/v1/reports")
    fun generateReport(): ResponseEntity<ReportResponse> =
        ReportResponse.Success(generateReportUseCase()).asEntity()

    sealed class ReportResponse : ApiResponse {

        data class Success(
            @JsonUnwrapped
            val report: Report,
        ) : ReportResponse()
    }
}
