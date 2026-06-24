CREATE TABLE IF NOT EXISTS acquisition_provider
(
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider_key          VARCHAR(64)  NOT NULL,
    name                  VARCHAR(255) NOT NULL,
    enabled               BOOLEAN      NOT NULL DEFAULT FALSE,
    base_url              VARCHAR(512),
    api_token             VARCHAR(1024),
    mode                  VARCHAR(32)  NOT NULL DEFAULT 'REQUEST',
    allowed_content_types VARCHAR(512),
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (provider_key)
);
