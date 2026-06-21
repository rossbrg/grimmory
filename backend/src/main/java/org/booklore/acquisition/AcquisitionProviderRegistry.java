package org.booklore.acquisition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Resolves {@link AcquisitionProvider} implementations by their key. This is the single place that
 * knows the set of available providers, keeping the rest of the core free of provider-specific
 * branching. New providers are added simply by declaring a Spring bean implementing
 * {@link AcquisitionProvider}.
 */
@Slf4j
@Component
public class AcquisitionProviderRegistry {

    private final Map<String, AcquisitionProvider> providersByKey;

    public AcquisitionProviderRegistry(List<AcquisitionProvider> providers) {
        Map<String, AcquisitionProvider> map = new LinkedHashMap<>();
        for (AcquisitionProvider provider : providers) {
            String key = provider.getProviderKey();
            AcquisitionProvider existing = map.putIfAbsent(key, provider);
            if (existing != null) {
                throw new IllegalStateException("Duplicate acquisition provider key: " + key);
            }
        }
        this.providersByKey = Map.copyOf(map);
        log.info("Registered {} acquisition provider(s): {}", providersByKey.size(), providersByKey.keySet());
    }

    public Optional<AcquisitionProvider> find(String providerKey) {
        if (providerKey == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(providersByKey.get(providerKey));
    }

    public boolean isKnown(String providerKey) {
        return providerKey != null && providersByKey.containsKey(providerKey);
    }

    public List<AcquisitionProvider> all() {
        return List.copyOf(providersByKey.values());
    }
}
