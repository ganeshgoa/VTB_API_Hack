// domain/Parameter.java
package com.apiscanner.domain;

public record Parameter(
    String name,
    ParameterType type, // QUERY, HEADER, PATH, BODY
    boolean required,
    String dataType,
    String format
) {}