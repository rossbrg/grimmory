package org.booklore.acquisition;

import java.util.List;

import org.booklore.acquisition.model.AcquisitionCandidate;
import org.booklore.acquisition.model.AcquisitionOutcome;
import org.booklore.acquisition.model.AcquisitionProviderContext;
import org.booklore.acquisition.model.AcquisitionQuery;
import org.booklore.model.enums.AcquisitionMode;

/**
 * The generic seam the whole acquisition feature hangs off. A provider knows how to talk to a single
 * external source (Shelfmark is the first implementation). Core code never branches on a concrete
 * provider; it resolves implementations through {@link AcquisitionProviderRegistry} by
 * {@link #getProviderKey()}.
 *
 * <p>Implementations must be stateless Spring beans. All per-deployment configuration arrives in the
 * {@link AcquisitionProviderContext}, never as bean state.</p>
 */
public interface AcquisitionProvider {

    /** Stable, lowercase key identifying this implementation, e.g. {@code "shelfmark"}. */
    String getProviderKey();

    /** Default human-readable name, used when an admin has not overridden it. */
    String getDisplayName();

    /**
     * Search the external source for candidates matching the query. Implementations should throw
     * {@link AcquisitionProviderException} on transport/auth failures rather than returning partial
     * state; the core decides how to surface that to the caller.
     */
    List<AcquisitionCandidate> search(AcquisitionProviderContext context, AcquisitionQuery query);

    /**
     * Trigger acquisition of a previously-returned candidate. {@code mode} selects direct-download vs
     * request behaviour; implementations that cannot honour the requested mode should fall back to
     * what they support and reflect that in the returned {@link AcquisitionOutcome}.
     */
    AcquisitionOutcome request(AcquisitionProviderContext context, AcquisitionCandidate candidate, AcquisitionMode mode);

    /**
     * Optional lightweight validation of resolved config before it is persisted/used. Default is a
     * no-op; providers override to assert required fields (base URL, token, etc.).
     */
    default void validate(AcquisitionProviderContext context) {
        // no-op by default
    }
}
