package org.booklore.acquisition;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Helpers for converting the comma-separated {@code allowed_content_types} column to/from a list,
 * normalizing to trimmed, upper-cased, de-duplicated values.
 */
public final class AcquisitionContentTypes {

    private AcquisitionContentTypes() {
    }

    public static List<String> toList(String csv) {
        List<String> result = new ArrayList<>();
        if (csv == null || csv.isBlank()) {
            return result;
        }
        Set<String> seen = new LinkedHashSet<>();
        for (String part : csv.split(",")) {
            String normalized = part.trim().toUpperCase();
            if (!normalized.isEmpty()) {
                seen.add(normalized);
            }
        }
        result.addAll(seen);
        return result;
    }

    public static String toCsv(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        Set<String> seen = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                seen.add(value.trim().toUpperCase());
            }
        }
        return seen.isEmpty() ? null : String.join(",", seen);
    }

    public static Set<String> toSet(String csv) {
        return new LinkedHashSet<>(toList(csv));
    }
}
