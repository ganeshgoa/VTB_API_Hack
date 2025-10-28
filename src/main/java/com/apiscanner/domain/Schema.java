package com.apiscanner.domain;

import java.util.Map;

public record Schema(
    String type,
    Map<String, Schema> properties,
    Schema items,
    String format,
    String description,
    boolean required
) {}