package com.apiscanner.domain;

import java.util.List;
import java.util.Map;

public record Endpoint(
    String path,
    HttpMethod method,
    String operationId,
    String summary,
    String description,
    List<Parameter> parameters,
    RequestBody requestBody,
    Map<String, Response> responses,
    List<String> securityRequirements,
    List<String> tags
) {}