package org.booklore.acquisition.provider.shelfmark;

import org.booklore.acquisition.AcquisitionProviderException;
import org.booklore.acquisition.model.AcquisitionCandidate;
import org.booklore.acquisition.model.AcquisitionOutcome;
import org.booklore.acquisition.model.AcquisitionProviderContext;
import org.booklore.acquisition.model.AcquisitionQuery;
import org.booklore.model.enums.AcquisitionMode;
import org.booklore.model.enums.AcquisitionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ShelfmarkAcquisitionProviderTest {

    private ShelfmarkAcquisitionProvider provider;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new ShelfmarkAcquisitionProvider(builder.build());
    }

    private AcquisitionProviderContext context(AcquisitionMode mode, Set<String> contentTypes) {
        return AcquisitionProviderContext.builder()
                .providerKey("shelfmark")
                .name("Shelfmark")
                .baseUrl("https://shelf.example.com/")
                .apiToken("secret-token")
                .defaultMode(mode)
                .allowedContentTypes(contentTypes)
                .build();
    }

    @Test
    void validate_requiresBaseUrl() {
        AcquisitionProviderContext noBase = AcquisitionProviderContext.builder().providerKey("shelfmark").build();
        assertThatThrownBy(() -> provider.validate(noBase))
                .isInstanceOf(AcquisitionProviderException.class)
                .hasMessageContaining("base URL");
    }

    @Test
    void search_mapsResults_sendsBearer_andFiltersByContentType() {
        String body = """
                {"results":[
                  {"id":"a1","title":"Dune","author":"Herbert","format":"epub","year":1965,"sizeBytes":1234,"downloadable":true},
                  {"id":"a2","title":"Dune Audio","author":"Herbert","format":"mp3","downloadable":false}
                ]}
                """;
        server.expect(requestTo("https://shelf.example.com/api/v1/search?q=dune"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer secret-token"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        List<AcquisitionCandidate> candidates =
                provider.search(context(AcquisitionMode.REQUEST, Set.of("EPUB")), AcquisitionQuery.ofText("dune"));

        server.verify();
        assertThat(candidates).hasSize(1);
        AcquisitionCandidate first = candidates.getFirst();
        assertThat(first.getExternalId()).isEqualTo("a1");
        assertThat(first.getFormat()).isEqualTo("EPUB");
        assertThat(first.getProviderKey()).isEqualTo("shelfmark");
        assertThat(first.isDirectDownloadAvailable()).isTrue();
    }

    @Test
    void search_noContentTypeRestriction_returnsAll() {
        String body = """
                {"results":[
                  {"id":"a1","title":"A","format":"epub","downloadable":true},
                  {"id":"a2","title":"B","format":"pdf","downloadable":true}
                ]}
                """;
        server.expect(requestTo("https://shelf.example.com/api/v1/search?q=test"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        List<AcquisitionCandidate> candidates =
                provider.search(context(AcquisitionMode.REQUEST, Set.of()), AcquisitionQuery.ofText("test"));

        assertThat(candidates).hasSize(2);
    }

    @Test
    void search_transportError_wrapsInProviderException() {
        server.expect(requestTo("https://shelf.example.com/api/v1/search?q=boom"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> provider.search(context(AcquisitionMode.REQUEST, Set.of()), AcquisitionQuery.ofText("boom")))
                .isInstanceOf(AcquisitionProviderException.class)
                .hasMessageContaining("search failed");
    }

    @Test
    void request_directDownload_postsPayloadAndMapsStatus() {
        server.expect(requestTo("https://shelf.example.com/api/v1/downloads"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer secret-token"))
                .andExpect(jsonPath("$.id").value("a1"))
                .andExpect(jsonPath("$.mode").value("DIRECT_DOWNLOAD"))
                .andRespond(withSuccess("{\"id\":\"dl-9\",\"status\":\"downloading\"}", MediaType.APPLICATION_JSON));

        AcquisitionCandidate candidate = AcquisitionCandidate.builder()
                .externalId("a1").directDownloadAvailable(true).build();

        AcquisitionOutcome outcome =
                provider.request(context(AcquisitionMode.REQUEST, Set.of()), candidate, AcquisitionMode.DIRECT_DOWNLOAD);

        server.verify();
        assertThat(outcome.getStatus()).isEqualTo(AcquisitionStatus.DOWNLOADING);
        assertThat(outcome.getExternalRef()).isEqualTo("dl-9");
    }

    @Test
    void request_directDownloadUnavailable_fallsBackToRequestMode() {
        server.expect(requestTo("https://shelf.example.com/api/v1/downloads"))
                .andExpect(jsonPath("$.mode").value("REQUEST"))
                .andRespond(withSuccess("{\"id\":\"dl-1\",\"status\":\"queued\"}", MediaType.APPLICATION_JSON));

        AcquisitionCandidate candidate = AcquisitionCandidate.builder()
                .externalId("a1").directDownloadAvailable(false).build();

        AcquisitionOutcome outcome =
                provider.request(context(AcquisitionMode.REQUEST, Set.of()), candidate, AcquisitionMode.DIRECT_DOWNLOAD);

        server.verify();
        assertThat(outcome.getStatus()).isEqualTo(AcquisitionStatus.QUEUED);
    }

    @Test
    void request_missingExternalId_throws() {
        AcquisitionCandidate candidate = AcquisitionCandidate.builder().build();
        assertThatThrownBy(() -> provider.request(context(AcquisitionMode.REQUEST, Set.of()), candidate, AcquisitionMode.REQUEST))
                .isInstanceOf(AcquisitionProviderException.class)
                .hasMessageContaining("external id");
    }
}
