package org.booklore.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import org.booklore.model.enums.AcquisitionMode;

/**
 * Admin-managed configuration for a single acquisition provider. {@code providerKey} selects the
 * {@link org.booklore.acquisition.AcquisitionProvider} implementation; the remaining columns are
 * that provider's per-deployment settings. New rows default to {@code enabled = false}.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "acquisition_provider")
public class AcquisitionProviderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_key", nullable = false, unique = true)
    private String providerKey;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = false;

    @Column(name = "base_url")
    private String baseUrl;

    @Column(name = "api_token")
    private String apiToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 32)
    @Builder.Default
    private AcquisitionMode mode = AcquisitionMode.REQUEST;

    /** Comma-separated list of allowed content types (e.g. {@code "EPUB,PDF"}); empty means no restriction. */
    @Column(name = "allowed_content_types")
    private String allowedContentTypes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
