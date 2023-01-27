package com.healthmetrix.recontact.message.controller

import com.healthmetrix.recontact.commons.CitizenId
import com.healthmetrix.recontact.persistence.message.api.Message
import com.healthmetrix.recontact.persistence.message.api.MessageRepository
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
@Profile("debug")
class MessageDebugController(
    private val messageRepository: MessageRepository,
) {

    @PostMapping("/v1/debug/messages")
    fun upsertForDebugging(
        @RequestBody
        message: Message,
    ) {
        messageRepository.upsert(message)
    }

    /**
     * Get messages for a citizen without and implicit state changes
     */
    @GetMapping("/v1/debug/messages")
    fun upsertForDebugging(
        @RequestHeader("X-Recontact-Citizen-Id")
        citizenId: CitizenId,
    ) = messageRepository.findAllByCitizenIdAndState(citizenId, null)
}
