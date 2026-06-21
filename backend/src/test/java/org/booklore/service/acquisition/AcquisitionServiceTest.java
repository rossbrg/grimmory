package org.booklore.service.acquisition;

import org.booklore.acquisition.AcquisitionProvider;
import org.booklore.acquisition.AcquisitionProviderException;
import org.booklore.acquisition.AcquisitionProviderRegistry;
import org.booklore.acquisition.model.AcquisitionCandidate;
import org.booklore.acquisition.model.AcquisitionOutcome;
import org.booklore.acquisition.model.AcquisitionProviderContext;
import org.booklore.config.security.service.AuthenticationService;
import org.booklore.exception.APIException;
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
import org.booklore.service.audit.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcquisitionServiceTest {

    @Mock
    private AcquisitionProviderService providerService;
    @Mock
    private AcquisitionProviderRegistry registry;
    @Mock
    private AcquisitionRequestRepository requestRepository;
    @Mock
    private AcquisitionRequestMapper requestMapper;
    @Mock
    private AuthenticationService authService;
    @Mock
    private AuditService auditService;
    @Mock
    private AcquisitionProvider provider;

    @InjectMocks
    private AcquisitionService service;

    private BookLoreUser user(boolean admin) {
        BookLoreUser.UserPermissions perms = new BookLoreUser.UserPermissions();
        perms.setAdmin(admin);
        perms.setCanDownload(true);
        return BookLoreUser.builder().id(5L).username("alice").permissions(perms).build();
    }

    private AcquisitionProviderEntity enabledShelfmark() {
        return AcquisitionProviderEntity.builder()
                .id(1L).providerKey("shelfmark").name("Shelfmark").enabled(true)
                .mode(AcquisitionMode.REQUEST).build();
    }

    @BeforeEach
    void setUp() {
        lenient().when(requestMapper.toDto(any())).thenReturn(new AcquisitionRequestDto());
        lenient().when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void search_noEnabledProviders_isNoOp() {
        when(providerService.getEnabledProviderEntities()).thenReturn(List.of());

        List<AcquisitionCandidate> result = service.searchExternalCandidates("dune");

        assertThat(result).isEmpty();
        verifyNoInteractions(registry);
    }

    @Test
    void search_aggregatesCandidatesFromEnabledProviders() {
        AcquisitionProviderEntity entity = enabledShelfmark();
        when(providerService.getEnabledProviderEntities()).thenReturn(List.of(entity));
        when(registry.find("shelfmark")).thenReturn(java.util.Optional.of(provider));
        when(providerService.toContext(entity)).thenReturn(mock(AcquisitionProviderContext.class));
        when(provider.search(any(), any())).thenReturn(List.of(
                AcquisitionCandidate.builder().providerKey("shelfmark").externalId("a1").title("Dune").build()));

        List<AcquisitionCandidate> result = service.searchExternalCandidates("dune");

        assertThat(result).singleElement().extracting(AcquisitionCandidate::getExternalId).isEqualTo("a1");
    }

    @Test
    void search_failingProviderIsSwallowed() {
        AcquisitionProviderEntity entity = enabledShelfmark();
        when(providerService.getEnabledProviderEntities()).thenReturn(List.of(entity));
        when(registry.find("shelfmark")).thenReturn(java.util.Optional.of(provider));
        when(providerService.toContext(entity)).thenReturn(mock(AcquisitionProviderContext.class));
        when(provider.search(any(), any())).thenThrow(new AcquisitionProviderException("boom"));

        assertThat(service.searchExternalCandidates("dune")).isEmpty();
    }

    @Test
    void trigger_disabledProvider_throws() {
        when(authService.getAuthenticatedUser()).thenReturn(user(false));
        when(providerService.getEnabledProviderEntities()).thenReturn(List.of());

        AcquisitionTriggerRequest req = AcquisitionTriggerRequest.builder()
                .providerKey("shelfmark").externalId("a1").build();

        assertThatThrownBy(() -> service.trigger(req))
                .isInstanceOf(APIException.class)
                .hasMessageContaining("not enabled");
        verify(requestRepository, never()).save(any());
    }

    @Test
    void trigger_success_persistsStatusAndAudits() {
        AcquisitionProviderEntity entity = enabledShelfmark();
        when(authService.getAuthenticatedUser()).thenReturn(user(false));
        when(providerService.getEnabledProviderEntities()).thenReturn(List.of(entity));
        when(registry.find("shelfmark")).thenReturn(java.util.Optional.of(provider));
        when(providerService.toContext(entity)).thenReturn(mock(AcquisitionProviderContext.class));
        when(provider.request(any(), any(), eq(AcquisitionMode.REQUEST)))
                .thenReturn(AcquisitionOutcome.queued("dl-42"));

        AcquisitionTriggerRequest req = AcquisitionTriggerRequest.builder()
                .providerKey("shelfmark").externalId("a1").title("Dune").author("Herbert").build();

        service.trigger(req);

        ArgumentCaptor<AcquisitionRequestEntity> captor = ArgumentCaptor.forClass(AcquisitionRequestEntity.class);
        verify(requestRepository).save(captor.capture());
        AcquisitionRequestEntity saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(AcquisitionStatus.QUEUED);
        assertThat(saved.getExternalRef()).isEqualTo("dl-42");
        assertThat(saved.getRequestedByUserId()).isEqualTo(5L);
        assertThat(saved.getRequestedByUsername()).isEqualTo("alice");
        verify(auditService).log(eq(AuditAction.ACQUISITION_REQUESTED), eq("AcquisitionRequest"), any(), anyString());
    }

    @Test
    void trigger_providerFailure_recordsFailedStatusWithoutThrowing() {
        AcquisitionProviderEntity entity = enabledShelfmark();
        when(authService.getAuthenticatedUser()).thenReturn(user(false));
        when(providerService.getEnabledProviderEntities()).thenReturn(List.of(entity));
        when(registry.find("shelfmark")).thenReturn(java.util.Optional.of(provider));
        when(providerService.toContext(entity)).thenReturn(mock(AcquisitionProviderContext.class));
        when(provider.request(any(), any(), any())).thenThrow(new AcquisitionProviderException("upstream 500"));

        AcquisitionTriggerRequest req = AcquisitionTriggerRequest.builder()
                .providerKey("shelfmark").externalId("a1").build();

        service.trigger(req);

        ArgumentCaptor<AcquisitionRequestEntity> captor = ArgumentCaptor.forClass(AcquisitionRequestEntity.class);
        verify(requestRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AcquisitionStatus.FAILED);
        assertThat(captor.getValue().getErrorMessage()).contains("upstream 500");
    }

    @Test
    void getRequests_adminSeesAll_otherSeesOwn() {
        when(authService.getAuthenticatedUser()).thenReturn(user(true));
        when(requestRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());
        service.getRequests();
        verify(requestRepository).findAllByOrderByCreatedAtDesc();

        reset(requestRepository, requestMapper);
        when(authService.getAuthenticatedUser()).thenReturn(user(false));
        when(requestRepository.findAllByRequestedByUserIdOrderByCreatedAtDesc(5L)).thenReturn(List.of());
        service.getRequests();
        verify(requestRepository).findAllByRequestedByUserIdOrderByCreatedAtDesc(5L);
    }
}
