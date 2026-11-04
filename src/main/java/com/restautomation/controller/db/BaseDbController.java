package com.restautomation.controller.db;

import com.restautomation.commons.db.ReadDbQueryFromYAML;
import com.restautomation.commons.utilities.dbhelpers.SnowFlakeDbConnector;
import com.restautomation.commons.utilities.enumhelpers.DataType;
import com.restautomation.commons.utilities.enumhelpers.EndPoints;

import java.util.List;
import java.util.Map;

/**
 * Thin per-API controller: looks up the SQL for (apiName, endpoint variant) in queries.yml and runs it.
 * Subclasses only declare their {@link DataType}; add methods for API-specific queries.
 */
public abstract class BaseDbController {

    protected final ReadDbQueryFromYAML queries;
    protected final SnowFlakeDbConnector connector;

    protected BaseDbController(ReadDbQueryFromYAML queries, SnowFlakeDbConnector connector) {
        this.queries = queries;
        this.connector = connector;
    }

    public abstract DataType getDataType();

    public <T> List<T> fetchRecords(EndPoints endpoint, List<String> params, boolean optOutFilter) {
        return connector.getQueryResultList(sql(endpoint.getQueryKey()), params, getDataType(), optOutFilter);
    }

    public <T> List<T> fetchRecords(String variant, List<String> params, boolean optOutFilter) {
        return connector.getQueryResultList(sql(variant), params, getDataType(), optOutFilter);
    }

    public List<Map<String, Object>> fetchRawRows(String variant, List<String> params, boolean optOutFilter) {
        return connector.executeQuery(sql(variant), params, optOutFilter);
    }

    protected String sql(String variant) {
        return queries.getQuery(getDataType().getApiName(), variant);
    }
}
