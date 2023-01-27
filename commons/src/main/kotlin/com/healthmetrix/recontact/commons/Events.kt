package com.healthmetrix.recontact.commons

class RequestUpdatedEvent(val requestId: RequestId, val updateType: UpdateType)

class CohortInfoChanged(val issueId: String)

enum class UpdateType {
    MESSAGE_STATE_CHANGED, REQUEST_CREATED, REQUEST_CANCELLED
}
