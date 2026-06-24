package org.booklore.service.acquisition;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.booklore.acquisition.AcquisitionContentTypes;
import org.booklore.acquisition.AcquisitionProvider;
import org.booklore.acquisition.AcquisitionProviderException;
import org.booklore.acquisition.AcquisitionProviderRegistry;
import org.booklore.acquisition.model.AcquisitionProviderContext;
import org.booklore.exception.ApiError;
import org.booklore.mapper.AcquisitionProviderMapper;
import org.booklore.model.dto.AcquisitionProviderConfig;
import org.booklore.model.dto.AvailableAcquisitionProvider;
import org.booklore.model.dto.request.AcquisitionProviderRequest;
import org.booklore.model.entity.AcquisitionProviderEntity;
import org.booklore.model.enums.AcquisitionMode;
import org.booklore.model.enums.AuditAction;
import org.booklore.repository.AcquisitionProviderRepository;
import org.booklore.service.audit.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Admin-managed CRUD for acquisition provider configuration. Validates each config against the
 * matching {@link AcquisitionProvider} from the registry so core code never branches on a concrete
 * provider. New configs default to disabled.
 */
@Slf4j
@Service
@AllArgsConstructor
public class AcquisitionProviderService {

    private final AcquisitionProviderRepository repository;
    private final AcquisitionProviderMapper mapper;
    private final AcquisitionProviderRegistry registry;
    private final AuditService auditService;

    public List<AcquisitionProviderConfig> getProviders() {
        return repository.findAll().stream().map(mapper::toDto).toList();
    }

    public AcquisitionProviderConfig getProvider(Long id) {
        return mapper.toDto(getEntity(id));
    }

    /** The provider implementations available in this build, flagged with whether a config exists. */
    public List<AvailableAcquisitionProvider> getAvailableProviders() {
        return registry.all().stream()
                .map(p -> AvailableAcquisitionProvider.builder()
                        .providerKey(p.getProviderKey())
                        .displayName(p.getDisplayName())
                        .configured(repository.existsByProviderKey(p.getProviderKey()))
                        .build())
                .toList();
    }

    @Transactional
    public AcquisitionProviderConfig createProvider(AcquisitionProviderRequest request) {
        AcquisitionProvider provider = registry.find(request.getProviderKey())
                .orElseThrow(() -> ApiError.ACQUISITION_PROVIDER_KEY_UNKNOWN.createException(request.getProviderKey()));
        if (repository.existsByProviderKey(request.getProviderKey())) {
            throw ApiError.ACQUISITION_PROVIDER_KEY_EXISTS.createException(request.getProviderKey());
        }

        AcquisitionProviderEntity entity = AcquisitionProviderEntity.builder()
                .providerKey(request.getProviderKey())
                .name(request.getName())
                .enabled(Boolean.TRUE.equals(request.getEnabled()))
                .baseUrl(trimToNull(request.getBaseUrl()))
                .apiToken(trimToNull(request.getApiToken()))
                .mode(request.getMode() != null ? request.getMode() : AcquisitionMode.REQUEST)
                .allowedContentTypes(AcquisitionContentTypes.toCsv(request.getAllowedContentTypes()))
                .build();

        validateIfEnabled(provider, entity);

        AcquisitionProviderEntity saved = repository.save(entity);
        auditService.log(AuditAction.ACQUISITION_PROVIDER_CREATED, "AcquisitionProvider", saved.getId(),
                "Created acquisition provider: " + saved.getProviderKey() + " (enabled=" + saved.isEnabled() + ")");
        return mapper.toDto(saved);
    }

    @Transactional
    public AcquisitionProviderConfig updateProvider(Long id, AcquisitionProviderRequest request) {
        AcquisitionProviderEntity entity = getEntity(id);
        AcquisitionProvider provider = registry.find(entity.getProviderKey())
                .orElseThrow(() -> ApiError.ACQUISITION_PROVIDER_KEY_UNKNOWN.createException(entity.getProviderKey()));

        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getEnabled() != null) {
            entity.setEnabled(request.getEnabled());
        }
        if (request.getBaseUrl() != null) {
            entity.setBaseUrl(trimToNull(request.getBaseUrl()));
        }
        if (request.getMode() != null) {
            entity.setMode(request.getMode());
        }
        if (request.getAllowedContentTypes() != null) {
            entity.setAllowedContentTypes(AcquisitionContentTypes.toCsv(request.getAllowedContentTypes()));
        }
        // Token semantics: null => keep existing, blank => clear, value => replace.
        if (request.getApiToken() != null) {
            entity.setApiToken(trimToNull(request.getApiToken()));
        }

        validateIfEnabled(provider, entity);

        AcquisitionProviderEntity saved = repository.save(entity);
        auditService.log(AuditAction.ACQUISITION_PROVIDER_UPDATED, "AcquisitionProvider", saved.getId(),
                "Updated acquisition provider: " + saved.getProviderKey() + " (enabled=" + saved.isEnabled() + ")");
        return mapper.toDto(saved);
    }

    @Transactional
    public void deleteProvider(Long id) {
        AcquisitionProviderEntity entity = getEntity(id);
        repository.delete(entity);
        auditService.log(AuditAction.ACQUISITION_PROVIDER_DELETED, "AcquisitionProvider", id,
                "Deleted acquisition provider: " + entity.getProviderKey());
    }

    /** Build the immutable context a provider operates with, from a persisted config. */
    public AcquisitionProviderContext toContext(AcquisitionProviderEntity entity) {
        return AcquisitionProviderContext.builder()
                .providerKey(entity.getProviderKey())
                .name(entity.getName())
                .baseUrl(entity.getBaseUrl())
                .apiToken(entity.getApiToken())
                .defaultMode(entity.getMode())
                .allowedContentTypes(AcquisitionContentTypes.toSet(entity.getAllowedContentTypes()))
                .build();
    }

    public List<AcquisitionProviderEntity> getEnabledProviderEntities() {
        return repository.findAllByEnabledTrue();
    }

    private AcquisitionProviderEntity getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiError.ACQUISITION_PROVIDER_NOT_FOUND.createException(id));
    }

    private void validateIfEnabled(AcquisitionProvider provider, AcquisitionProviderEntity entity) {
        if (!entity.isEnabled()) {
            return;
        }
        try {
            provider.validate(toContext(entity));
        } catch (AcquisitionProviderException e) {
            throw ApiError.GENERIC_BAD_REQUEST.createException(e.getMessage());
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
