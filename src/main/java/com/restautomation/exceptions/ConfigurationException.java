package com.restautomation.exceptions;

/** Missing or inconsistent configuration (YAML entry, property, enum registration). */
public class ConfigurationException extends FrameworkException {

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
