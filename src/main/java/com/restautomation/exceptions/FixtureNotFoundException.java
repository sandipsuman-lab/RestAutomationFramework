package com.restautomation.exceptions;

/** A fixture file, or a key inside one, does not exist. Raised immediately instead of returning null. */
public class FixtureNotFoundException extends FrameworkException {

    public FixtureNotFoundException(String message) {
        super(message);
    }

    public FixtureNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
