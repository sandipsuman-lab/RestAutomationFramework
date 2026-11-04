package com.restautomation.commons.db;

import com.restautomation.exceptions.ConfigurationException;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SQL variants for one API, keyed by variant name ({@code byTenant}, {@code byDateRange}, ...).
 * A map rather than one field per variant, so a new variant needs YAML only.
 */
@Data
public class DatabaseQueries {
    private String apiName;
    private Map<String, String> databaseQueries = new LinkedHashMap<>();

    public String getQuery(String variant) {
        String sql = databaseQueries.get(variant);
        if (sql == null || sql.isBlank()) {
            throw new ConfigurationException("No query '" + variant + "' for apiName '" + apiName
                    + "' in queries.yml. Available: " + databaseQueries.keySet());
        }
        return sql.trim();
    }
}
