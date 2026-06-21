package org.booklore.acquisition.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.booklore.model.enums.AcquisitionStatus;

/**
 * The provider's response to an acquisition request: the initial lifecycle status, an optional
 * provider-side reference for later status checks, and a human-readable message.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcquisitionOutcome {
    private AcquisitionStatus status;
    private String externalRef;
    private String message;

    public static AcquisitionOutcome queued(String externalRef) {
        return AcquisitionOutcome.builder().status(AcquisitionStatus.QUEUED).externalRef(externalRef).build();
    }

    public static AcquisitionOutcome failed(String message) {
        return AcquisitionOutcome.builder().status(AcquisitionStatus.FAILED).message(message).build();
    }
}
