package org.booklore.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import org.booklore.model.enums.AcquisitionMode;
import org.booklore.model.enums.AcquisitionStatus;

/**
 * An acquisition triggered by a user. Carries a snapshot of the chosen candidate plus the lifecycle
 * status, so the request remains meaningful even if the external provider's catalogue changes.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "acquisition_request")
public class AcquisitionRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider_key", nullable = false)
    private String providerKey;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(name = "title")
    private String title;

    @Column(name = "author")
    private String author;

    @Column(name = "format")
    private String format;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 32)
    private AcquisitionMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AcquisitionStatus status;

    /** Provider-side reference (e.g. a Shelfmark download id) for later status checks. */
    @Column(name = "external_ref")
    private String externalRef;

    @Column(name = "error_message", length = 1024)
    private String errorMessage;

    /** Set once the acquired item has been imported and matched to a local book. */
    @Column(name = "result_book_id")
    private Long resultBookId;

    @Column(name = "requested_by_user_id")
    private Long requestedByUserId;

    @Column(name = "requested_by_username")
    private String requestedByUsername;

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
