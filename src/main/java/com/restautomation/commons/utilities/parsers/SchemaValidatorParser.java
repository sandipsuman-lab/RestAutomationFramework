package com.restautomation.commons.utilities.parsers;

import com.restautomation.commons.utilities.common.FileUtils;
import com.restautomation.commons.utilities.enumhelpers.DataType;
import com.restautomation.config.ReadConfigYaml;
import com.restautomation.exceptions.ConfigurationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** {@link DataType} → expected-response fixture folder and JSON Schema paths. */
@Component
@RequiredArgsConstructor
public class SchemaValidatorParser {

    private final ReadConfigYaml config;

    /** Folder name passed to {@code ReadJsonFile.getJsonData(folder, file, key)}. */
    public String getSchemaFolder(DataType type) {
        if (type.getSchemaFolder() == null) {
            throw new ConfigurationException("No schema folder configured for " + type);
        }
        return type.getSchemaFolder();
    }

    /** Classpath location of a JSON Schema document. */
    public String getJsonSchemaPath(String fileName) {
        return fileName.contains("/") ? fileName : FileUtils.join(config.getPaths().getJsonSchemas(), fileName);
    }
}
