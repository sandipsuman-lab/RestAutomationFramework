package com.restautomation.commons.utilities.enumhelpers;

import com.restautomation.exceptions.ConfigurationException;
import lombok.Getter;

import java.util.Arrays;

/** DB query variants. {@code queryKey} selects the SQL variant in {@code queries.yml}. */
@Getter
public enum EndPoints {

    DateRange("byDateRange"),
    History("byHistory"),
    Tenant("byTenant"),
    ById("byId");

    private final String queryKey;

    EndPoints(String queryKey) {
        this.queryKey = queryKey;
    }

    public static EndPoints fromName(String name) {
        return Arrays.stream(values())
                .filter(e -> e.name().equalsIgnoreCase(name) || e.queryKey.equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new ConfigurationException("Unknown endpoint '" + name + "'. Known: " + Arrays.toString(values())));
    }
}
