package org.booklore.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import org.booklore.model.enums.AcquisitionMode;
import org.booklore.model.enums.AcquisitionStatus;

/**
 * User/admin-facing view of an acquisition request and its current lifecycle status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AcquisitionRequestDto {
    private Long id;
    private String providerKey;
    private String externalId;
    private String title;
    private String author;
    private String format;
    private AcquisitionMode mode;
    private AcquisitionStatus status;
    private String errorMessage;
    private Long resultBookId;
    private Long requestedByUserId;
    private String requestedByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
