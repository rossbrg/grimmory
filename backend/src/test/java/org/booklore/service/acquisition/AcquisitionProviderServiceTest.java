package org.booklore.service.acquisition;

import org.booklore.acquisition.AcquisitionProvider;
import org.booklore.acquisition.AcquisitionProviderException;
import org.booklore.acquisition.AcquisitionProviderRegistry;
import org.booklore.exception.APIException;
import org.booklore.mapper.AcquisitionProviderMapper;
import org.booklore.model.dto.AcquisitionProviderConfig;
import org.booklore.model.dto.request.AcquisitionProviderRequest;
import org.booklore.model.entity.AcquisitionProviderEntity;
import org.booklore.model.enums.AcquisitionMode;
import org.booklore.model.enums.AuditAction;
import org.booklore.repository.AcquisitionProviderRepository;
import org.booklore.service.audit.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcquisitionProviderServiceTest {

    @Mock
    private AcquisitionProviderRepository repository;
    @Mock
    private AcquisitionProviderMapper mapper;
    @Mock
    private AcquisitionProviderRegistry registry;
    @Mock
    private AuditService auditService;
    @Mock
    private AcquisitionProvider shelfmark;

    @InjectMocks
    private AcquisitionProviderService service;

    @BeforeEach
    void setUp() {
        lenient().when(mapper.toDto(any())).thenReturn(new AcquisitionProviderConfig());
        lenient().when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private AcquisitionProviderRequest request() {
        return AcquisitionProviderRequest.builder()
                .providerKey("shelfmark")
                .name("Shelfmark")
                .enabled(false)
                .baseUrl("https://shelf.example.com")
                .apiToken("tok")
                .mode(AcquisitionMode.REQUEST)
                .allowedContentTypes(List.of("epub", "pdf"))
                .build();
    }

    @Test
    void create_unknownProviderKey_throws() {
        when(registry.find("shelfmark")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createProvider(request()))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("Unknown acquisition provider");
        verify(repository, never()).save(any());
    }

    @Test
    void create_duplicateProviderKey_throws() {
        when(registry.find("shelfmark")).thenReturn(Optional.of(shelfmark));
        when(repository.existsByProviderKey("shelfmark")).thenReturn(true);

        assertThatThrownBy(() -> service.createProvider(request()))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("already configured");
        verify(repository, never()).save(any());
    }

    @Test
    void create_disabled_skipsValidation_persistsAndAudits() {
        when(registry.find("shelfmark")).thenReturn(Optional.of(shelfmark));
        when(repository.existsByProviderKey("shelfmark")).thenReturn(false);

        service.createProvider(request());

        verify(shelfmark, never()).validate(any());
        ArgumentCaptor<AcquisitionProviderEntity> captor = ArgumentCaptor.forClass(AcquisitionProviderEntity.class);
        verify(repository).save(captor.capture());
        AcquisitionProviderEntity saved = captor.getValue();
        assertThat(saved.isEnabled()).isFalse();
        assertThat(saved.getAllowedContentTypes()).isEqualTo("EPUB,PDF");
        assertThat(saved.getApiToken()).isEqualTo("tok");
        verify(auditService).log(eq(AuditAction.ACQUISITION_PROVIDER_CREATED), eq("AcquisitionProvider"), any(), anyString());
    }

    @Test
    void create_enabled_invalidConfig_throwsBadRequest() {
        AcquisitionProviderRequest req = request();
        req.setEnabled(true);
        when(registry.find("shelfmark")).thenReturn(Optional.of(shelfmark));
        when(repository.existsByProviderKey("shelfmark")).thenReturn(false);
        doThrow(new AcquisitionProviderException("base URL is required")).when(shelfmark).validate(any());

        assertThatThrownBy(() -> service.createProvider(req))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("base URL is required");
        verify(repository, never()).save(any());
    }

    @Test
    void update_tokenSemantics_nullKeeps_blankClears_valueReplaces() {
        AcquisitionProviderEntity existing = AcquisitionProviderEntity.builder()
                .id(7L).providerKey("shelfmark").name("Shelfmark").enabled(false)
                .apiToken("old").mode(AcquisitionMode.REQUEST).build();
        when(registry.find("shelfmark")).thenReturn(Optional.of(shelfmark));

        // null token -> keep
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        AcquisitionProviderRequest keep = AcquisitionProviderRequest.builder().providerKey("shelfmark").name("Shelfmark").build();
        service.updateProvider(7L, keep);
        assertThat(existing.getApiToken()).isEqualTo("old");

        // blank token -> clear
        AcquisitionProviderRequest clear = AcquisitionProviderRequest.builder().providerKey("shelfmark").name("Shelfmark").apiToken("   ").build();
        service.updateProvider(7L, clear);
        assertThat(existing.getApiToken()).isNull();

        // value -> replace
        AcquisitionProviderRequest replace = AcquisitionProviderRequest.builder().providerKey("shelfmark").name("Shelfmark").apiToken("new").build();
        service.updateProvider(7L, replace);
        assertThat(existing.getApiToken()).isEqualTo("new");

        verify(auditService, times(3)).log(eq(AuditAction.ACQUISITION_PROVIDER_UPDATED), any(), any(), anyString());
    }

    @Test
    void delete_audits() {
        AcquisitionProviderEntity existing = AcquisitionProviderEntity.builder().id(3L).providerKey("shelfmark").name("S").build();
        when(repository.findById(3L)).thenReturn(Optional.of(existing));

        service.deleteProvider(3L);

        verify(repository).delete(existing);
        verify(auditService).log(eq(AuditAction.ACQUISITION_PROVIDER_DELETED), eq("AcquisitionProvider"), eq(3L), anyString());
    }

    @Test
    void availableProviders_flagConfiguredState() {
        when(registry.all()).thenReturn(List.of(shelfmark));
        when(shelfmark.getProviderKey()).thenReturn("shelfmark");
        when(shelfmark.getDisplayName()).thenReturn("Shelfmark");
        when(repository.existsByProviderKey("shelfmark")).thenReturn(true);

        var available = service.getAvailableProviders();

        assertThat(available).singleElement().satisfies(p -> {
            assertThat(p.getProviderKey()).isEqualTo("shelfmark");
            assertThat(p.isConfigured()).isTrue();
        });
    }
}
