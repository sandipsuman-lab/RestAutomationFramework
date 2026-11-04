package com.restautomation.commons.utilities.common;

import java.util.Map;

/** URL composition helpers. */
public final class UrlUtils {

    private UrlUtils() {
    }

    /** Joins a base URL and a relative path with exactly one slash; absolute paths are returned unchanged. */
    public static String join(String baseUrl, String path) {
        if (path == null || path.isBlank()) {
            return baseUrl;
        }
        if (isAbsolute(path)) {
            return path;
        }
        return baseUrl.replaceAll("/+$", "") + "/" + path.replaceAll("^/+", "");
    }

    public static boolean isAbsolute(String url) {
        return url != null && url.matches("(?i)^https?://.*");
    }

    /** Replaces {name} tokens in a URL template. */
    public static String expand(String template, Map<String, ?> values) {
        String result = template;
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return result;
    }
}
