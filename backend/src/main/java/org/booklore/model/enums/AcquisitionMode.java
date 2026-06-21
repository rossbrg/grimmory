package org.booklore.model.enums;

/**
 * Determines how an {@link org.booklore.acquisition.AcquisitionProvider} fulfils an acquisition.
 *
 * <ul>
 *     <li>{@link #DIRECT_DOWNLOAD} - the provider downloads the item immediately and imports it into a shared root.</li>
 *     <li>{@link #REQUEST} - the provider records a request to be fulfilled later (e.g. a moderated queue).</li>
 * </ul>
 */
public enum AcquisitionMode {
    DIRECT_DOWNLOAD,
    REQUEST
}
