package com.restautomation.commons.utilities.common;

import java.util.Collection;
import java.util.regex.Pattern;

/** Masks secrets before they reach logs or reports. */
public final class MaskingUtils {

    private static final Pattern SECRET_JSON_FIELDS = Pattern.compile(
            "(\"(?:access_token|refresh_token|client_secret|password|apiKey)\"\\s*:\\s*\")([^\"]+)(\")",
            Pattern.CASE_INSENSITIVE);

    private MaskingUtils() {
    }

    /** Keeps the first 4 characters (and a "Bearer " prefix) so tokens stay distinguishable. */
    public static String mask(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String prefix = "";
        String secret = value;
        if (value.regionMatches(true, 0, "Bearer ", 0, 7)) {
            prefix = value.substring(0, 7);
            secret = value.substring(7);
        }
        String visible = secret.length() > 8 ? secret.substring(0, 4) : "";
        return prefix + visible + "****";
    }

    public static String maskIfSensitive(String name, String value, Collection<String> sensitiveNames) {
        return sensitiveNames.stream().anyMatch(name::equalsIgnoreCase) ? mask(value) : value;
    }

    public static String maskJsonSecrets(String body) {
        return body == null ? null : SECRET_JSON_FIELDS.matcher(body).replaceAll("$1****$3");
    }
}
