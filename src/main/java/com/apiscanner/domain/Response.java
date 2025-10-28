package com.apiscanner.domain;

public record Response(
    String statusCode,
    String description,
    Schema schema
) {}