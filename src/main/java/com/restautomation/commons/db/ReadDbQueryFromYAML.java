package com.restautomation.commons.db;

import com.restautomation.commons.utilities.readerhelpers.YamlResourceReader;
import com.restautomation.exceptions.ConfigurationException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Loads {@code queries.yml} into {@code Map<apiName, DatabaseQueries>} (cached). */
@Component
@RequiredArgsConstructor
public class ReadDbQueryFromYAML {

    public static final String QUERIES_FILE = "queries.yml";

    private final YamlResourceReader yamlReader;
    private volatile Map<String, DatabaseQueries> queries;

    public Map<String, DatabaseQueries> getQueries() {
        if (queries == null) {
            synchronized (this) {
                if (queries == null) {
                    Map<String, DatabaseQueries> map = new LinkedHashMap<>();
                    yamlReader.read(QUERIES_FILE, QueriesYaml.class).getSnowflakeQueries()
                            .forEach(entry -> map.put(entry.getApiName(), entry));
                    queries = map;
                }
            }
        }
        return queries;
    }

    public String getQuery(String apiName, String variant) {
        DatabaseQueries entry = getQueries().get(apiName);
        if (entry == null) {
            throw new ConfigurationException("No queries for apiName '" + apiName + "' in " + QUERIES_FILE
                    + ". Configured: " + getQueries().keySet());
        }
        return entry.getQuery(variant);
    }

    @Data
    public static class QueriesYaml {
        private List<DatabaseQueries> snowflakeQueries = new ArrayList<>();
    }
}
