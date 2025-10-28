// domain/Endpoint.java
package com.apiscanner.domain;

import java.util.List;
import java.util.Map;
public record Endpoint(
    String path,
    HttpMethod method,
    Map<String, Parameter> parameters,
    String requestSchema,
    String responseSchema,
    List<String> securityRequirements
) {}