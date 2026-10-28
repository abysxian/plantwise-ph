package com.agristep.weather;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/** A place found by name search. */
public record GeoPlace(String name, String region, String country, double latitude, double longitude) {

    public String label() {
        return Stream.of(name, region, country)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining(", "));
    }
}
