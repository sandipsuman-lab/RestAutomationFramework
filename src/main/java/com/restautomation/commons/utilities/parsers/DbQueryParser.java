package com.restautomation.commons.utilities.parsers;

import com.restautomation.commons.utilities.enumhelpers.DataType;
import com.restautomation.commons.utilities.enumhelpers.EndPoints;
import com.restautomation.controller.db.BaseDbController;
import com.restautomation.exceptions.ConfigurationException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Central DB dispatcher. Every {@link BaseDbController} bean is registered by its {@link DataType},
 * so adding an API needs no change here.
 */
@Component
public class DbQueryParser {

    private final Map<DataType, BaseDbController> controllers = new EnumMap<>(DataType.class);

    public DbQueryParser(List<BaseDbController> dbControllers) {
        dbControllers.forEach(controller -> controllers.put(controller.getDataType(), controller));
    }

    public <T> List<T> executeDbQueryForDataTypeByEndpoint(DataType type, EndPoints endpoint, List<String> params,
                                                           boolean optOutFlag) {
        return controllerFor(type).fetchRecords(endpoint, params, optOutFlag);
    }

    /** Raw rows of a named query variant, for generic (model-free) comparisons. */
    public List<Map<String, Object>> executeNamedQuery(DataType type, String variant, List<String> params, boolean optOutFlag) {
        return controllerFor(type).fetchRawRows(variant, params, optOutFlag);
    }

    public BaseDbController controllerFor(DataType type) {
        BaseDbController controller = controllers.get(type);
        if (controller == null) {
            throw new ConfigurationException("No DB controller registered for " + type
                    + ". Create a @Component extending BaseDbController for it.");
        }
        return controller;
    }
}
