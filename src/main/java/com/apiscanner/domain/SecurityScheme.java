// domain/SecurityScheme.java
package com.apiscanner.domain;


public record SecurityScheme(
    String name,
    SecurityType type, // API_KEY, HTTP, OAUTH2
    String in, // header, query, cookie
    String scheme // bearer, basic, etc.
) {}