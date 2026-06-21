package org.booklore.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.booklore.model.enums.AcquisitionMode;

/**
 * Caller payload to trigger acquisition of a candidate previously returned by
 * {@code GET /api/v1/acquisition/search}. Mirrors the candidate essentials so the request can be
 * snapshotted and replayed to the provider.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcquisitionTriggerRequest {

    @NotBlank
    private String providerKey;

    @NotBlank
    private String externalId;

    private String title;
    private String author;
    private String format;
    private boolean directDownloadAvailable;

    /** Optional override; when null the provider's configured default mode is used. */
    private AcquisitionMode mode;
}
