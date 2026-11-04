package com.restautomation.exceptions;

/** The data needed for a validation could not be obtained (e.g. an expected-data query returned nothing). */
public class DataValidationException extends FrameworkException {

    public DataValidationException(String message) {
        super(message);
    }

    public DataValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
