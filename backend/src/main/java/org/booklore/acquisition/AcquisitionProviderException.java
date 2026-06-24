package org.booklore.acquisition;

/**
 * Raised by an {@link AcquisitionProvider} when an external call fails or the provider configuration
 * is invalid. The core translates this into a clean caller-facing error and a {@code FAILED} status.
 */
public class AcquisitionProviderException extends RuntimeException {

    public AcquisitionProviderException(String message) {
        super(message);
    }

    public AcquisitionProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
