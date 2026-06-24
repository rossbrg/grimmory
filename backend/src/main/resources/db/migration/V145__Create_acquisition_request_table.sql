CREATE TABLE IF NOT EXISTS acquisition_request
(
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider_key          VARCHAR(64)   NOT NULL,
    external_id           VARCHAR(512)  NOT NULL,
    title                 VARCHAR(1024),
    author                VARCHAR(512),
    format                VARCHAR(32),
    mode                  VARCHAR(32)   NOT NULL,
    status                VARCHAR(32)   NOT NULL,
    external_ref          VARCHAR(512),
    error_message         VARCHAR(1024),
    result_book_id        BIGINT,
    requested_by_user_id  BIGINT,
    requested_by_username VARCHAR(255),
    created_at            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_acquisition_request_created_at ON acquisition_request (created_at);
CREATE INDEX idx_acquisition_request_user_id ON acquisition_request (requested_by_user_id);
CREATE INDEX idx_acquisition_request_status ON acquisition_request (status);
