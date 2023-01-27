package com.healthmetrix.recontact.commons

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

interface ApiResponse {

    val status: HttpStatus
        @JsonIgnore
        get() = HttpStatus.OK

    val hasBody: Boolean
        @JsonIgnore
        get() = true

    val headers: HttpHeaders
        @JsonIgnore
        get() = HttpHeaders.EMPTY
}

fun <T : ApiResponse> T.asEntity(): ResponseEntity<T> {
    return ResponseEntity
        .status(status)
        .headers(headers)
        .body(if (hasBody) this else null)
}
