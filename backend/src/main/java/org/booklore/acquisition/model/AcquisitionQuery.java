package org.booklore.acquisition.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A normalized search request handed to an {@link org.booklore.acquisition.AcquisitionProvider}.
 * Providers may use the free-text {@link #text} or the structured fields, whichever they support.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcquisitionQuery {
    private String text;
    private String title;
    private String author;
    private String isbn;

    public static AcquisitionQuery ofText(String text) {
        return AcquisitionQuery.builder().text(text).build();
    }
}
