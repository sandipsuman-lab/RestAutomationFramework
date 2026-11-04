package com.restautomation.commons.utilities.enumhelpers;

import com.restautomation.exceptions.ConfigurationException;

import java.util.Arrays;

/** HTTP verbs used by request dispatch. */
public enum RequestType {
    Get, Post, Put, Patch, Delete, Head, Options;

    public static RequestType fromName(String name) {
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new ConfigurationException("Unsupported HTTP method '" + name + "'"));
    }
}
