package org.booklore.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import org.booklore.acquisition.model.AcquisitionCandidate;

/**
 * External acquisition candidates for a query, kept deliberately separate from local library search
 * results so the UI can present "not in library" candidates distinctly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AcquisitionSearchResponse {
    private String query;
    private List<AcquisitionCandidate> candidates;
}
