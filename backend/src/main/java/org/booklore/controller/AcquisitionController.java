package org.booklore.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.booklore.model.dto.AcquisitionProviderConfig;
import org.booklore.model.dto.AcquisitionRequestDto;
import org.booklore.model.dto.AcquisitionSearchResponse;
import org.booklore.model.dto.AvailableAcquisitionProvider;
import org.booklore.model.dto.request.AcquisitionProviderRequest;
import org.booklore.model.dto.request.AcquisitionTriggerRequest;
import org.booklore.service.acquisition.AcquisitionProviderService;
import org.booklore.service.acquisition.AcquisitionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST surface for the acquisition feature.
 *
 * <p>Provider configuration is admin-only. Searching for and triggering acquisitions is gated on the
 * download permission (or admin), matching how the rest of BookLore treats fetching content.</p>
 */
@AllArgsConstructor
@RestController
@RequestMapping("/api/v1/acquisition")
@Tag(name = "Acquisition", description = "Integrated missing-book acquisition via pluggable providers")
public class AcquisitionController {

    private final AcquisitionProviderService providerService;
    private final AcquisitionService acquisitionService;

    @Operation(summary = "List configured acquisition providers")
    @ApiResponse(responseCode = "200", description = "Providers returned successfully")
    @PreAuthorize("@securityUtil.isAdmin()")
    @GetMapping("/providers")
    public ResponseEntity<List<AcquisitionProviderConfig>> getProviders() {
        return ResponseEntity.ok(providerService.getProviders());
    }

    @Operation(summary = "List provider implementations available in this build")
    @ApiResponse(responseCode = "200", description = "Available providers returned successfully")
    @PreAuthorize("@securityUtil.isAdmin()")
    @GetMapping("/providers/available")
    public ResponseEntity<List<AvailableAcquisitionProvider>> getAvailableProviders() {
        return ResponseEntity.ok(providerService.getAvailableProviders());
    }

    @Operation(summary = "Get a configured acquisition provider")
    @ApiResponse(responseCode = "200", description = "Provider returned successfully")
    @PreAuthorize("@securityUtil.isAdmin()")
    @GetMapping("/providers/{id}")
    public ResponseEntity<AcquisitionProviderConfig> getProvider(@PathVariable Long id) {
        return ResponseEntity.ok(providerService.getProvider(id));
    }

    @Operation(summary = "Configure a new acquisition provider")
    @ApiResponse(responseCode = "201", description = "Provider created successfully")
    @PreAuthorize("@securityUtil.isAdmin()")
    @PostMapping("/providers")
    public ResponseEntity<AcquisitionProviderConfig> createProvider(@RequestBody @Valid AcquisitionProviderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(providerService.createProvider(request));
    }

    @Operation(summary = "Update an acquisition provider")
    @ApiResponse(responseCode = "200", description = "Provider updated successfully")
    @PreAuthorize("@securityUtil.isAdmin()")
    @PutMapping("/providers/{id}")
    public ResponseEntity<AcquisitionProviderConfig> updateProvider(@PathVariable Long id,
                                                                    @RequestBody @Valid AcquisitionProviderRequest request) {
        return ResponseEntity.ok(providerService.updateProvider(id, request));
    }

    @Operation(summary = "Delete an acquisition provider")
    @ApiResponse(responseCode = "204", description = "Provider deleted successfully")
    @PreAuthorize("@securityUtil.isAdmin()")
    @DeleteMapping("/providers/{id}")
    public ResponseEntity<Void> deleteProvider(@PathVariable Long id) {
        providerService.deleteProvider(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Search enabled providers for acquisition candidates",
            description = "Returns external candidates from enabled providers. A no-op (empty list) when no provider is enabled.")
    @ApiResponse(responseCode = "200", description = "Candidates returned successfully")
    @PreAuthorize("@securityUtil.canDownload() or @securityUtil.isAdmin()")
    @GetMapping("/search")
    public ResponseEntity<AcquisitionSearchResponse> search(
            @Parameter(description = "Free-text search query") @RequestParam String q) {
        AcquisitionSearchResponse response = AcquisitionSearchResponse.builder()
                .query(q)
                .candidates(acquisitionService.searchExternalCandidates(q))
                .build();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Trigger acquisition of a candidate")
    @ApiResponse(responseCode = "201", description = "Acquisition request recorded")
    @PreAuthorize("@securityUtil.canDownload() or @securityUtil.isAdmin()")
    @PostMapping("/requests")
    public ResponseEntity<AcquisitionRequestDto> trigger(@RequestBody @Valid AcquisitionTriggerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(acquisitionService.trigger(request));
    }

    @Operation(summary = "List acquisition requests",
            description = "Admins see all requests; other permitted users see only their own.")
    @ApiResponse(responseCode = "200", description = "Requests returned successfully")
    @PreAuthorize("@securityUtil.canDownload() or @securityUtil.isAdmin()")
    @GetMapping("/requests")
    public ResponseEntity<List<AcquisitionRequestDto>> getRequests() {
        return ResponseEntity.ok(acquisitionService.getRequests());
    }

    @Operation(summary = "Get an acquisition request and its status")
    @ApiResponse(responseCode = "200", description = "Request returned successfully")
    @PreAuthorize("@securityUtil.canDownload() or @securityUtil.isAdmin()")
    @GetMapping("/requests/{id}")
    public ResponseEntity<AcquisitionRequestDto> getRequest(@PathVariable Long id) {
        return ResponseEntity.ok(acquisitionService.getRequest(id));
    }
}
