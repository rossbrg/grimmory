package org.booklore.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import org.booklore.model.enums.AcquisitionMode;

/**
 * Admin-facing view of a configured acquisition provider. The API token is never returned; callers
 * see only {@link #tokenSet} so the UI can show whether a secret is stored without exposing it.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AcquisitionProviderConfig {
    private Long id;
    private String providerKey;
    private String name;
    private boolean enabled;
    private String baseUrl;
    private AcquisitionMode mode;
    private List<String> allowedContentTypes;
    private boolean tokenSet;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
