package org.booklore.service.acquisition;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.booklore.acquisition.AcquisitionProvider;
import org.booklore.acquisition.AcquisitionProviderException;
import org.booklore.acquisition.AcquisitionProviderRegistry;
import org.booklore.acquisition.model.AcquisitionCandidate;
import org.booklore.acquisition.model.AcquisitionOutcome;
import org.booklore.acquisition.model.AcquisitionProviderContext;
import org.booklore.acquisition.model.AcquisitionQuery;
import org.booklore.config.security.service.AuthenticationService;
import org.booklore.exception.ApiError;
import org.booklore.mapper.AcquisitionRequestMapper;
import org.booklore.model.dto.AcquisitionRequestDto;
import org.booklore.model.dto.BookLoreUser;
import org.booklore.model.dto.request.AcquisitionTriggerRequest;
import org.booklore.model.entity.AcquisitionProviderEntity;
import org.booklore.model.entity.AcquisitionRequestEntity;
import org.booklore.model.enums.AcquisitionMode;
import org.booklore.model.enums.AcquisitionStatus;
import org.booklore.model.enums.AuditAction;
import org.booklore.repository.AcquisitionRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the acquisition feature end-to-end: aggregates external candidates from enabled
 * providers (ROS-397), triggers acquisitions and records their lifecycle status with an audit trail
 * (ROS-399). All provider resolution goes through the registry, keeping this core provider-agnostic.
 */
@Slf4j
@Service
@AllArgsConstructor
public class AcquisitionService {

    private final AcquisitionProviderService providerService;
    private final AcquisitionProviderRegistry registry;
    private final AcquisitionRequestRepository requestRepository;
    private final AcquisitionRequestMapper requestMapper;
    private final AuthenticationService authService;
    private final org.booklore.service.audit.AuditService auditService;

    /**
     * Aggregate external candidates from every enabled provider. Returns an empty list (a no-op) when
     * no provider is enabled, and never lets a single failing provider break the whole search.
     */
    public List<AcquisitionCandidate> searchExternalCandidates(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        List<AcquisitionProviderEntity> enabled = providerService.getEnabledProviderEntities();
        if (enabled.isEmpty()) {
            return List.of();
        }

        AcquisitionQuery acquisitionQuery = AcquisitionQuery.ofText(query.trim());
        List<AcquisitionCandidate> candidates = new ArrayList<>();
        for (AcquisitionProviderEntity entity : enabled) {
            AcquisitionProvider provider = registry.find(entity.getProviderKey()).orElse(null);
            if (provider == null) {
                log.warn("Enabled acquisition provider '{}' has no registered implementation; skipping", entity.getProviderKey());
                continue;
            }
            AcquisitionProviderContext context = providerService.toContext(entity);
            try {
                candidates.addAll(provider.search(context, acquisitionQuery));
            } catch (AcquisitionProviderException e) {
                log.warn("Acquisition provider '{}' search failed: {}", entity.getProviderKey(), e.getMessage());
            }
        }
        return candidates;
    }

    /**
     * Trigger acquisition of a candidate. The request is always persisted (even on provider failure)
     * so its status is observable; provider failures are captured as {@code FAILED} rather than thrown.
     */
    @Transactional
    public AcquisitionRequestDto trigger(AcquisitionTriggerRequest request) {
        BookLoreUser user = authService.getAuthenticatedUser();

        AcquisitionProviderEntity providerEntity = providerService.getEnabledProviderEntities().stream()
                .filter(e -> e.getProviderKey().equals(request.getProviderKey()))
                .findFirst()
                .orElseThrow(() -> ApiError.ACQUISITION_PROVIDER_DISABLED.createException(request.getProviderKey()));

        AcquisitionProvider provider = registry.find(request.getProviderKey())
                .orElseThrow(() -> ApiError.ACQUISITION_PROVIDER_KEY_UNKNOWN.createException(request.getProviderKey()));

        AcquisitionMode mode = request.getMode() != null ? request.getMode() : providerEntity.getMode();

        AcquisitionRequestEntity entity = AcquisitionRequestEntity.builder()
                .providerKey(request.getProviderKey())
                .externalId(request.getExternalId())
                .title(request.getTitle())
                .author(request.getAuthor())
                .format(request.getFormat())
                .mode(mode)
                .status(AcquisitionStatus.QUEUED)
                .requestedByUserId(user.getId())
                .requestedByUsername(user.getUsername())
                .build();

        AcquisitionCandidate candidate = AcquisitionCandidate.builder()
                .providerKey(request.getProviderKey())
                .externalId(request.getExternalId())
                .title(request.getTitle())
                .author(request.getAuthor())
                .format(request.getFormat())
                .directDownloadAvailable(request.isDirectDownloadAvailable())
                .build();

        try {
            AcquisitionOutcome outcome = provider.request(providerService.toContext(providerEntity), candidate, mode);
            entity.setStatus(outcome.getStatus() != null ? outcome.getStatus() : AcquisitionStatus.QUEUED);
            entity.setExternalRef(outcome.getExternalRef());
            if (entity.getStatus() == AcquisitionStatus.FAILED) {
                entity.setErrorMessage(outcome.getMessage());
            }
        } catch (AcquisitionProviderException e) {
            log.warn("Acquisition request to provider '{}' failed: {}", request.getProviderKey(), e.getMessage());
            entity.setStatus(AcquisitionStatus.FAILED);
            entity.setErrorMessage(e.getMessage());
        }

        AcquisitionRequestEntity saved = requestRepository.save(entity);
        auditService.log(AuditAction.ACQUISITION_REQUESTED, "AcquisitionRequest", saved.getId(),
                "Requested acquisition via " + saved.getProviderKey() + " [" + saved.getStatus() + "]: "
                        + (saved.getTitle() != null ? saved.getTitle() : saved.getExternalId()));
        return requestMapper.toDto(saved);
    }

    public List<AcquisitionRequestDto> getRequests() {
        BookLoreUser user = authService.getAuthenticatedUser();
        List<AcquisitionRequestEntity> entities = user.getPermissions().isAdmin()
                ? requestRepository.findAllByOrderByCreatedAtDesc()
                : requestRepository.findAllByRequestedByUserIdOrderByCreatedAtDesc(user.getId());
        return entities.stream().map(requestMapper::toDto).toList();
    }

    public AcquisitionRequestDto getRequest(Long id) {
        BookLoreUser user = authService.getAuthenticatedUser();
        AcquisitionRequestEntity entity = user.getPermissions().isAdmin()
                ? requestRepository.findById(id).orElseThrow(() -> ApiError.ACQUISITION_REQUEST_NOT_FOUND.createException(id))
                : requestRepository.findByIdAndRequestedByUserId(id, user.getId())
                        .orElseThrow(() -> ApiError.ACQUISITION_REQUEST_NOT_FOUND.createException(id));
        return requestMapper.toDto(entity);
    }

    /**
     * Advance a request's lifecycle status (e.g. from a provider callback or poller) and audit the
     * transition. Kept on the service so future async wiring has a single, audited entry point.
     */
    @Transactional
    public AcquisitionRequestDto updateStatus(Long id, AcquisitionStatus status, String errorMessage, Long resultBookId) {
        AcquisitionRequestEntity entity = requestRepository.findById(id)
                .orElseThrow(() -> ApiError.ACQUISITION_REQUEST_NOT_FOUND.createException(id));
        AcquisitionStatus previous = entity.getStatus();
        entity.setStatus(status);
        if (errorMessage != null) {
            entity.setErrorMessage(errorMessage);
        }
        if (resultBookId != null) {
            entity.setResultBookId(resultBookId);
        }
        AcquisitionRequestEntity saved = requestRepository.save(entity);
        auditService.log(AuditAction.ACQUISITION_STATUS_CHANGED, "AcquisitionRequest", saved.getId(),
                "Acquisition status " + previous + " -> " + status);
        return requestMapper.toDto(saved);
    }
}
