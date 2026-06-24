package org.booklore.acquisition.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;

import org.booklore.model.enums.AcquisitionMode;

/**
 * Resolved, immutable configuration handed to a provider for a single operation. Built by the core
 * from the persisted provider config so that {@link org.booklore.acquisition.AcquisitionProvider}
 * implementations stay stateless and decoupled from persistence.
 */
@Getter
@Builder
@AllArgsConstructor
public class AcquisitionProviderContext {
    private final String providerKey;
    private final String name;
    private final String baseUrl;
    private final String apiToken;
    private final AcquisitionMode defaultMode;
    private final Set<String> allowedContentTypes;

    /** True when the provider may serve the given content type, or when no restriction is configured. */
    public boolean allowsContentType(String contentType) {
        if (allowedContentTypes == null || allowedContentTypes.isEmpty()) {
            return true;
        }
        if (contentType == null) {
            return false;
        }
        return allowedContentTypes.stream().anyMatch(t -> t.equalsIgnoreCase(contentType));
    }
}
