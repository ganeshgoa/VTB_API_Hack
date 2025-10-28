package com.apiscanner.domain;

public record RequestBody(
    String contentType,
    Schema schema,
    String description
) {}