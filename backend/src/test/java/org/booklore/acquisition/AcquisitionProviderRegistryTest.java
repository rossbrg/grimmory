package org.booklore.acquisition;

import org.booklore.acquisition.model.AcquisitionCandidate;
import org.booklore.acquisition.model.AcquisitionOutcome;
import org.booklore.acquisition.model.AcquisitionProviderContext;
import org.booklore.acquisition.model.AcquisitionQuery;
import org.booklore.model.enums.AcquisitionMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AcquisitionProviderRegistryTest {

    private static AcquisitionProvider stub(String key) {
        return new AcquisitionProvider() {
            @Override
            public String getProviderKey() {
                return key;
            }

            @Override
            public String getDisplayName() {
                return key;
            }

            @Override
            public List<AcquisitionCandidate> search(AcquisitionProviderContext context, AcquisitionQuery query) {
                return List.of();
            }

            @Override
            public AcquisitionOutcome request(AcquisitionProviderContext context, AcquisitionCandidate candidate, AcquisitionMode mode) {
                return AcquisitionOutcome.queued("ref");
            }
        };
    }

    @Test
    void resolvesProvidersByKey() {
        AcquisitionProviderRegistry registry = new AcquisitionProviderRegistry(List.of(stub("shelfmark"), stub("other")));

        assertThat(registry.find("shelfmark")).isPresent();
        assertThat(registry.isKnown("other")).isTrue();
        assertThat(registry.isKnown("missing")).isFalse();
        assertThat(registry.find(null)).isEmpty();
        assertThat(registry.all()).hasSize(2);
    }

    @Test
    void duplicateKeysAreRejected() {
        assertThatThrownBy(() -> new AcquisitionProviderRegistry(List.of(stub("dup"), stub("dup"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dup");
    }
}
