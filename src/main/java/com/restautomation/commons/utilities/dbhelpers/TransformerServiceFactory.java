package com.restautomation.commons.utilities.dbhelpers;

import com.restautomation.commons.utilities.enumhelpers.DataType;
import com.restautomation.exceptions.ConfigurationException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of every {@link BaseTransformerService} bean, keyed by {@link DataType}.
 * New transformers register themselves just by being Spring beans.
 */
@Component
public class TransformerServiceFactory {

    private final Map<DataType, BaseTransformerService<?>> services = new EnumMap<>(DataType.class);

    public TransformerServiceFactory(List<BaseTransformerService<?>> transformerServices) {
        transformerServices.forEach(service -> {
            BaseTransformerService<?> previous = services.put(service.getDataType(), service);
            if (previous != null) {
                throw new ConfigurationException("Two DB transformers registered for " + service.getDataType() + ": "
                        + previous.getClass().getSimpleName() + ", " + service.getClass().getSimpleName());
            }
        });
    }

    @SuppressWarnings("unchecked")
    public <T> BaseTransformerService<T> get(DataType type) {
        BaseTransformerService<?> service = services.get(type);
        if (service == null) {
            throw new ConfigurationException("No DB transformer registered for " + type
                    + ". Create a @Service implementing BaseTransformerService for it.");
        }
        return (BaseTransformerService<T>) service;
    }
}
