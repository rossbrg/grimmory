package org.booklore.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A provider implementation available in this build (from the registry), offered to admins when
 * configuring a new provider. Independent of whether a config row exists yet.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableAcquisitionProvider {
    private String providerKey;
    private String displayName;
    private boolean configured;
}
