CREATE TABLE request
(
    id VARCHAR NOT NULL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    cohort_id VARCHAR NOT NULL,
    project_id VARCHAR NOT NULL,
    project_name VARCHAR NOT NULL,
    message_content_text VARCHAR NOT NULL,
    active BOOLEAN  NOT NULL
);

CREATE TABLE message
(
    id uuid NOT NULL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    linked_request VARCHAR NOT NULL,
    state VARCHAR  NOT NULL,
    recipient_id VARCHAR NOT NULL,
    content_text VARCHAR NOT NULL
);

CREATE TABLE citizen_cohort
(
    cohort_id VARCHAR NOT NULL,
    citizen_id VARCHAR NOT NULL,
    CONSTRAINT unique_cohort_id_citizen_id UNIQUE(cohort_id, citizen_id)
);