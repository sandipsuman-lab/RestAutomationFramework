package com.restautomation.commons.utilities.dbhelpers;

import com.fasterxml.jackson.databind.JsonNode;
import com.restautomation.commons.utilities.common.JsonUtils;
import com.restautomation.commons.utilities.enumhelpers.DataType;
import com.restautomation.commons.utilities.parsers.ParseTableNames;
import com.restautomation.config.ReadConfigYaml;
import com.restautomation.exceptions.DataValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Reader;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes SQL against the configured {@code DataSource} (Snowflake in real environments, H2 locally)
 * and returns normalised rows: lower-case column labels, timestamps as ISO-8601 UTC strings,
 * dates as {@code yyyy-MM-dd}, CLOBs as text.
 * <p>
 * Queries may contain {@value #OPT_OUT_TOKEN}; it is replaced with {@code AND <optOutCondition>}
 * when the subscription has privacy opt-out enabled, and removed otherwise.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SnowFlakeDbConnector {

    public static final String OPT_OUT_TOKEN = "{{OPT_OUT_FILTER}}";

    private final JdbcTemplate jdbcTemplate;
    private final TransformerServiceFactory transformerFactory;
    private final ReadConfigYaml config;

    public List<Map<String, Object>> executeQuery(String sql, List<?> params) {
        log.info("Executing query on {} with params {}", ParseTableNames.parse(sql), params);
        return jdbcTemplate.execute((ConnectionCallback<List<Map<String, Object>>>) connection -> {
            if (isSnowflake(connection)) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("ALTER SESSION SET JDBC_QUERY_RESULT_FORMAT='JSON'");
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setQueryTimeout(config.getDb().getQueryTimeoutSeconds());
                bind(statement, params);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return readRows(resultSet);
                }
            }
        });
    }

    public List<Map<String, Object>> executeQuery(String sql, List<?> params, boolean optOutFilter) {
        return executeQuery(applyOptOutFilter(sql, optOutFilter), params);
    }

    /** Rows mapped to the DB model registered for {@code type}. */
    public <T> List<T> getQueryResultList(String sql, List<?> params, DataType type, boolean optOutFilter) {
        BaseTransformerService<T> transformer = transformerFactory.get(type);
        return executeQuery(sql, params, optOutFilter).stream()
                .map(row -> transformer.transform(JsonUtils.mapper().valueToTree(row)))
                .toList();
    }

    /** First row mapped to the DB model; fails if the query returns nothing. */
    public <T> T getQueryResult(String sql, List<?> params, DataType type, boolean optOutFilter) {
        List<T> rows = getQueryResultList(sql, params, type, optOutFilter);
        if (rows.isEmpty()) {
            throw new DataValidationException("Query returned no rows for " + type + " with params " + params);
        }
        return rows.get(0);
    }

    /** Rows as JSON objects, for callers that compare generically. */
    public List<JsonNode> getQueryResultAsJson(String sql, List<?> params, boolean optOutFilter) {
        return executeQuery(sql, params, optOutFilter).stream()
                .map(row -> (JsonNode) JsonUtils.mapper().valueToTree(row))
                .toList();
    }

    public Object queryForScalar(String sql, List<?> params) {
        List<Map<String, Object>> rows = executeQuery(sql, params);
        return rows.isEmpty() || rows.get(0).isEmpty() ? null : rows.get(0).values().iterator().next();
    }

    public long count(String sql, List<?> params) {
        Object value = queryForScalar(sql, params);
        return value == null ? 0 : Long.parseLong(value.toString());
    }

    /** INSERT/UPDATE/DELETE for test-data setup and clean-up. */
    public int executeUpdate(String sql, List<?> params) {
        log.info("Executing update on {} with params {}", ParseTableNames.parse(sql), params);
        return jdbcTemplate.update(sql, params == null ? new Object[0] : params.toArray());
    }

    public String applyOptOutFilter(String sql, boolean optOutFilter) {
        String condition = optOutFilter ? " AND " + config.getDb().getOptOutCondition() : "";
        if (sql.contains(OPT_OUT_TOKEN)) {
            return sql.replace(OPT_OUT_TOKEN, condition);
        }
        if (optOutFilter) {
            log.warn("Query has no {} placeholder; appending the opt-out condition at the end", OPT_OUT_TOKEN);
            return sql + condition;
        }
        return sql;
    }

    private static void bind(PreparedStatement statement, List<?> params) throws SQLException {
        if (params == null) {
            return;
        }
        for (int i = 0; i < params.size(); i++) {
            statement.setObject(i + 1, params.get(i));
        }
    }

    private static List<Map<String, Object>> readRows(ResultSet resultSet) throws SQLException {
        ResultSetMetaData meta = resultSet.getMetaData();
        List<Map<String, Object>> rows = new ArrayList<>();
        while (resultSet.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int c = 1; c <= meta.getColumnCount(); c++) {
                row.put(meta.getColumnLabel(c).toLowerCase(), normalise(resultSet.getObject(c)));
            }
            rows.add(row);
        }
        return rows;
    }

    /** Timestamps are treated as UTC wall-clock values, independent of the JVM time zone. */
    private static Object normalise(Object value) throws SQLException {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toInstant(ZoneOffset.UTC).toString();
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.toInstant(ZoneOffset.UTC).toString();
        }
        if (value instanceof OffsetDateTime dateTime) {
            return dateTime.toInstant().toString();
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate().toString();
        }
        if (value instanceof LocalDate date) {
            return date.toString();
        }
        if (value instanceof Time time) {
            return time.toLocalTime().toString();
        }
        if (value instanceof Clob clob) {
            try (Reader reader = clob.getCharacterStream()) {
                StringBuilder text = new StringBuilder();
                char[] buffer = new char[4096];
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    text.append(buffer, 0, read);
                }
                return text.toString();
            } catch (IOException e) {
                throw new SQLException("Unable to read CLOB", e);
            }
        }
        if (value instanceof byte[] bytes) {
            return Base64.getEncoder().encodeToString(bytes);
        }
        return value;
    }

    private static boolean isSnowflake(Connection connection) throws SQLException {
        return connection.getMetaData().getDatabaseProductName().toLowerCase().contains("snowflake");
    }
}
