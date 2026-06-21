package org.booklore.mapper;

import org.booklore.model.dto.AcquisitionProviderConfig;
import org.booklore.model.entity.AcquisitionProviderEntity;
import org.booklore.model.enums.AcquisitionMode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AcquisitionProviderMapperTest {

    private final AcquisitionProviderMapper mapper = new AcquisitionProviderMapperImpl();

    @Test
    void toDto_masksToken_andSplitsContentTypes() {
        AcquisitionProviderEntity entity = AcquisitionProviderEntity.builder()
                .id(1L)
                .providerKey("shelfmark")
                .name("Shelfmark")
                .enabled(true)
                .baseUrl("https://shelf.example.com")
                .apiToken("super-secret")
                .mode(AcquisitionMode.DIRECT_DOWNLOAD)
                .allowedContentTypes("EPUB,PDF")
                .build();

        AcquisitionProviderConfig dto = mapper.toDto(entity);

        assertThat(dto.getProviderKey()).isEqualTo("shelfmark");
        assertThat(dto.getAllowedContentTypes()).containsExactly("EPUB", "PDF");
        assertThat(dto.isTokenSet()).isTrue();
        // The DTO has no apiToken field at all, so the secret cannot leak.
    }

    @Test
    void toDto_tokenSetFalse_whenNoToken() {
        AcquisitionProviderEntity entity = AcquisitionProviderEntity.builder()
                .id(2L).providerKey("shelfmark").name("S").mode(AcquisitionMode.REQUEST).build();

        AcquisitionProviderConfig dto = mapper.toDto(entity);

        assertThat(dto.isTokenSet()).isFalse();
        assertThat(dto.getAllowedContentTypes()).isEmpty();
    }
}
