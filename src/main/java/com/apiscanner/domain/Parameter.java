package com.apiscanner.domain;

public record Parameter(
    String name,
    String in,  // query, header, path, cookie
    boolean required,
    String dataType,
    String format,
    String description
) {
    public Parameter {
        // Default values for nulls
        if (format == null) format = "";
        if (description == null) description = "";
    }
}