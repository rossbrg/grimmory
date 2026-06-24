package org.booklore.model.enums;

/**
 * Lifecycle states for an acquisition request, surfaced in the UI and audit trail.
 */
public enum AcquisitionStatus {
    /** Accepted by the provider and waiting to start (covers request-mode submissions too). */
    QUEUED,
    /** The provider is actively fetching the item. */
    DOWNLOADING,
    /** The item has been downloaded and imported into a shared library root. */
    IMPORTED,
    /** The acquisition failed; {@code errorMessage} explains why. */
    FAILED
}
