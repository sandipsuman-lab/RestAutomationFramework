package com.restautomation.commons.utilities.parsers;

import com.restautomation.commons.utilities.common.FileUtils;
import com.restautomation.commons.utilities.enumhelpers.DataType;
import com.restautomation.config.ReadConfigYaml;
import com.restautomation.exceptions.ConfigurationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** {@link DataType} → path of JSON request templates. */
@Component
@RequiredArgsConstructor
public class RequestPathParser {

    private final ReadConfigYaml config;

    public String getRequestTemplatePath(DataType type, String fileName) {
        if (type.getRequestFolder() == null) {
            throw new ConfigurationException("No request-template folder configured for " + type);
        }
        return FileUtils.join(config.getPaths().getRequestFiles(), type.getRequestFolder(), fileName);
    }
}
