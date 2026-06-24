package org.booklore.acquisition.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An external acquisition candidate returned by a provider. This is deliberately distinct from any
 * local library model so the search surface can keep local matches and provider candidates clearly
 * separated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AcquisitionCandidate {
    /** Key of the provider that produced this candidate (e.g. {@code "shelfmark"}). */
    private String providerKey;
    /** Provider-scoped identifier used to trigger acquisition of this exact item. */
    private String externalId;
    private String title;
    private String author;
    private String format;
    private String language;
    private Long sizeBytes;
    private Integer year;
    private String coverUrl;
    private String description;
    /** Whether the provider can fulfil this candidate via direct download (vs request-only). */
    private boolean directDownloadAvailable;
}
