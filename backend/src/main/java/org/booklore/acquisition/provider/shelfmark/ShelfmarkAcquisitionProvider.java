package org.booklore.acquisition.provider.shelfmark;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.booklore.acquisition.AcquisitionProvider;
import org.booklore.acquisition.AcquisitionProviderException;
import org.booklore.acquisition.model.AcquisitionCandidate;
import org.booklore.acquisition.model.AcquisitionOutcome;
import org.booklore.acquisition.model.AcquisitionProviderContext;
import org.booklore.acquisition.model.AcquisitionQuery;
import org.booklore.model.enums.AcquisitionMode;
import org.booklore.model.enums.AcquisitionStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * First {@link AcquisitionProvider}: integrates Shelfmark over its HTTP API. No Shelfmark code is
 * vendored; this is a pure API client.
 *
 * <p>Assumed API contract (configurable base URL, optional bearer token):</p>
 * <ul>
 *     <li>{@code GET  {baseUrl}/api/v1/search?q=...} &rarr; {@code { "results": [ {id,title,author,format,language,year,sizeBytes,coverUrl,description,downloadable} ] }}</li>
 *     <li>{@code POST {baseUrl}/api/v1/downloads}  body {@code {"id","mode"}} &rarr; {@code { "id", "status" }}</li>
 * </ul>
 */
@Slf4j
@Component
public class ShelfmarkAcquisitionProvider implements AcquisitionProvider {

    public static final String PROVIDER_KEY = "shelfmark";
    private static final String SEARCH_PATH = "/api/v1/search";
    private static final String DOWNLOADS_PATH = "/api/v1/downloads";

    private final RestClient restClient;

    public ShelfmarkAcquisitionProvider(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public String getProviderKey() {
        return PROVIDER_KEY;
    }

    @Override
    public String getDisplayName() {
        return "Shelfmark";
    }

    @Override
    public void validate(AcquisitionProviderContext context) {
        if (context.getBaseUrl() == null || context.getBaseUrl().isBlank()) {
            throw new AcquisitionProviderException("Shelfmark base URL is required");
        }
    }

    @Override
    public List<AcquisitionCandidate> search(AcquisitionProviderContext context, AcquisitionQuery query) {
        validate(context);
        String term = query.getText() != null && !query.getText().isBlank()
                ? query.getText()
                : joinStructured(query);
        if (term == null || term.isBlank()) {
            return List.of();
        }

        URI uri = UriComponentsBuilder.fromUriString(normalizeBase(context.getBaseUrl()))
                .path(SEARCH_PATH)
                .queryParam("q", term)
                .build()
                .encode()
                .toUri();

        ShelfmarkSearchResponse response;
        try {
            response = restClient.get()
                    .uri(uri)
                    .headers(headers -> applyAuth(headers, context))
                    .retrieve()
                    .body(ShelfmarkSearchResponse.class);
        } catch (RestClientException e) {
            throw new AcquisitionProviderException("Shelfmark search failed: " + e.getMessage(), e);
        }

        if (response == null || response.results() == null) {
            return List.of();
        }

        return response.results().stream()
                .map(this::toCandidate)
                .filter(candidate -> context.allowsContentType(candidate.getFormat()))
                .toList();
    }

    @Override
    public AcquisitionOutcome request(AcquisitionProviderContext context, AcquisitionCandidate candidate, AcquisitionMode mode) {
        validate(context);
        if (candidate == null || candidate.getExternalId() == null || candidate.getExternalId().isBlank()) {
            throw new AcquisitionProviderException("A candidate external id is required to request acquisition");
        }

        AcquisitionMode effectiveMode = resolveMode(candidate, mode, context);

        URI uri = UriComponentsBuilder.fromUriString(normalizeBase(context.getBaseUrl()))
                .path(DOWNLOADS_PATH)
                .build()
                .encode()
                .toUri();

        Map<String, String> payload = Map.of(
                "id", candidate.getExternalId(),
                "mode", effectiveMode.name()
        );

        ShelfmarkDownloadResponse response;
        try {
            response = restClient.post()
                    .uri(uri)
                    .headers(headers -> applyAuth(headers, context))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(ShelfmarkDownloadResponse.class);
        } catch (RestClientException e) {
            throw new AcquisitionProviderException("Shelfmark acquisition request failed: " + e.getMessage(), e);
        }

        if (response == null) {
            throw new AcquisitionProviderException("Shelfmark returned an empty acquisition response");
        }

        return AcquisitionOutcome.builder()
                .status(mapStatus(response.status()))
                .externalRef(response.id())
                .message("Submitted to Shelfmark in " + effectiveMode + " mode")
                .build();
    }

    private AcquisitionMode resolveMode(AcquisitionCandidate candidate, AcquisitionMode requested, AcquisitionProviderContext context) {
        AcquisitionMode mode = requested != null ? requested : context.getDefaultMode();
        if (mode == null) {
            mode = AcquisitionMode.REQUEST;
        }
        if (mode == AcquisitionMode.DIRECT_DOWNLOAD && !candidate.isDirectDownloadAvailable()) {
            log.debug("Candidate {} does not support direct download; falling back to REQUEST mode", candidate.getExternalId());
            return AcquisitionMode.REQUEST;
        }
        return mode;
    }

    private AcquisitionCandidate toCandidate(ShelfmarkResult result) {
        return AcquisitionCandidate.builder()
                .providerKey(PROVIDER_KEY)
                .externalId(result.id())
                .title(result.title())
                .author(result.author())
                .format(result.format() != null ? result.format().toUpperCase() : null)
                .language(result.language())
                .year(result.year())
                .sizeBytes(result.sizeBytes())
                .coverUrl(result.coverUrl())
                .description(result.description())
                .directDownloadAvailable(Boolean.TRUE.equals(result.downloadable()))
                .build();
    }

    private static AcquisitionStatus mapStatus(String status) {
        if (status == null) {
            return AcquisitionStatus.QUEUED;
        }
        return switch (status.trim().toLowerCase()) {
            case "downloading", "in_progress", "fetching" -> AcquisitionStatus.DOWNLOADING;
            case "imported", "completed", "done" -> AcquisitionStatus.IMPORTED;
            case "failed", "error" -> AcquisitionStatus.FAILED;
            default -> AcquisitionStatus.QUEUED;
        };
    }

    private static void applyAuth(HttpHeaders headers, AcquisitionProviderContext context) {
        if (context.getApiToken() != null && !context.getApiToken().isBlank()) {
            headers.setBearerAuth(context.getApiToken());
        }
    }

    private static String normalizeBase(String baseUrl) {
        String trimmed = baseUrl.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private static String joinStructured(AcquisitionQuery query) {
        StringBuilder sb = new StringBuilder();
        if (query.getTitle() != null) {
            sb.append(query.getTitle());
        }
        if (query.getAuthor() != null) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(query.getAuthor());
        }
        if (query.getIsbn() != null) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(query.getIsbn());
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ShelfmarkSearchResponse(List<ShelfmarkResult> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ShelfmarkResult(
            String id,
            String title,
            String author,
            String format,
            String language,
            Integer year,
            @JsonProperty("sizeBytes") Long sizeBytes,
            @JsonProperty("coverUrl") String coverUrl,
            String description,
            Boolean downloadable
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ShelfmarkDownloadResponse(String id, String status) {
    }
}
