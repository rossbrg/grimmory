package org.booklore.acquisition;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AcquisitionContentTypesTest {

    @Test
    void toList_normalizesTrimsUppercasesAndDeduplicates() {
        assertThat(AcquisitionContentTypes.toList(" epub , pdf ,EPUB"))
                .containsExactly("EPUB", "PDF");
    }

    @Test
    void toList_blankOrNull_returnsEmpty() {
        assertThat(AcquisitionContentTypes.toList(null)).isEmpty();
        assertThat(AcquisitionContentTypes.toList("  ")).isEmpty();
    }

    @Test
    void toCsv_normalizesAndJoins() {
        assertThat(AcquisitionContentTypes.toCsv(List.of("epub", "PDF", " epub ")))
                .isEqualTo("EPUB,PDF");
    }

    @Test
    void toCsv_emptyOrNull_returnsNull() {
        assertThat(AcquisitionContentTypes.toCsv(null)).isNull();
        assertThat(AcquisitionContentTypes.toCsv(List.of())).isNull();
        assertThat(AcquisitionContentTypes.toCsv(List.of("  "))).isNull();
    }

    @Test
    void toSet_roundTripsFromCsv() {
        assertThat(AcquisitionContentTypes.toSet("epub,pdf")).containsExactlyInAnyOrder("EPUB", "PDF");
    }
}
