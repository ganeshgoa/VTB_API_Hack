package com.apiscanner.domain;

import java.util.List;
import java.util.Map;

public record ApiSpec(
    String baseUrl,
    String title,
    String version,
    Map<String, List<Endpoint>> endpoints
) {}