package org.booklore.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import org.booklore.model.enums.AcquisitionMode;

/**
 * Create/update payload for an acquisition provider configuration. On update, a null {@code apiToken}
 * leaves the stored token unchanged; a blank string clears it.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcquisitionProviderRequest {

    @NotBlank
    private String providerKey;

    @NotBlank
    private String name;

    private Boolean enabled;
    private String baseUrl;
    private String apiToken;
    private AcquisitionMode mode;
    private List<String> allowedContentTypes;
}
