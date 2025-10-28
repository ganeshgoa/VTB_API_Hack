// loaders/OpenApiSpecLoader.java
package com.apiscanner.loaders;

import com.apiscanner.core.SpecLoader;
import com.apiscanner.domain.*;
import com.apiscanner.exceptions.SpecLoadException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class OpenApiSpecLoader implements SpecLoader {
    private final ObjectMapper jsonMapper;
    private final ObjectMapper yamlMapper;
    
    public OpenApiSpecLoader() {
        this.jsonMapper = new ObjectMapper();
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }
    
    @Override
    public ApiSpec loadFromFile(Path filePath) throws SpecLoadException {
        try {
            String content = Files.readString(filePath);
            return parseSpecContent(content, filePath.toString());
        } catch (Exception e) {
            throw new SpecLoadException("Failed to load spec from file: " + filePath, e);
        }
    }
    
    @Override
    public ApiSpec loadFromUrl(String url) throws SpecLoadException {
        try {
            // Simple HTTP client - можно заменить на OkHttp позже
            java.net.URL specUrl = new java.net.URL(url);
            try (java.util.Scanner scanner = new java.util.Scanner(specUrl.openStream())) {
                String content = scanner.useDelimiter("\\A").next();
                return parseSpecContent(content, url);
            }
        } catch (Exception e) {
            throw new SpecLoadException("Failed to load spec from URL: " + url, e);
        }
    }
    
    @Override
    public boolean supports(String contentType) {
        return contentType.contains("json") || 
               contentType.contains("yaml") || 
               contentType.contains("yml");
    }
    
    private ApiSpec parseSpecContent(String content, String source) throws Exception {
        JsonNode root = tryParse(content);
        
        String openapi = root.has("openapi") ? 
            root.get("openapi").asText() : 
            root.has("swagger") ? root.get("swagger").asText() : "unknown";
        
        String baseUrl = extractBaseUrl(root);
        String title = root.path("info").path("title").asText("Untitled API");
        String version = root.path("info").path("version").asText("1.0.0");
        
        Map<String, List<Endpoint>> endpoints = extractEndpoints(root);
        List<SecurityScheme> securitySchemes = extractSecuritySchemes(root);
        
        return new ApiSpec(baseUrl, title, version, endpoints, securitySchemes, content);
    }
    
    private JsonNode tryParse(String content) throws Exception {
        // Try JSON first, then YAML
        try {
            return jsonMapper.readTree(content);
        } catch (Exception e) {
            return yamlMapper.readTree(content);
        }
    }
    
    private String extractBaseUrl(JsonNode root) {
        JsonNode servers = root.get("servers");
        if (servers != null && servers.isArray() && servers.size() > 0) {
            return servers.get(0).path("url").asText("");
        }
        
        JsonNode host = root.get("host");
        if (host != null) {
            String scheme = root.has("schemes") ? 
                root.get("schemes").get(0).asText("https") : "https";
            String basePath = root.path("basePath").asText("");
            return scheme + "://" + host.asText() + basePath;
        }
        
        return "";
    }
    
    private Map<String, List<Endpoint>> extractEndpoints(JsonNode root) {
        Map<String, List<Endpoint>> endpoints = new HashMap<>();
        JsonNode paths = root.get("paths");
        
        if (paths != null) {
            paths.fields().forEachRemaining(pathEntry -> {
                String path = pathEntry.getKey();
                JsonNode pathItem = pathEntry.getValue();
                
                List<Endpoint> pathEndpoints = new ArrayList<>();
                pathItem.fields().forEachRemaining(methodEntry -> {
                    String methodName = methodEntry.getKey().toUpperCase();
                    if (isHttpMethod(methodName)) {
                        HttpMethod method = HttpMethod.valueOf(methodName);
                        JsonNode operation = methodEntry.getValue();
                        
                        Map<String, Parameter> parameters = extractParameters(operation);
                        List<String> securityRequirements = extractSecurityRequirements(operation);
                        
                        String requestSchema = extractSchema(operation, "requestBody");
                        String responseSchema = extractSchema(operation, "responses");
                        
                        Endpoint endpoint = new Endpoint(
                            path, method, parameters, requestSchema, 
                            responseSchema, securityRequirements
                        );
                        pathEndpoints.add(endpoint);
                    }
                });
                
                endpoints.put(path, pathEndpoints);
            });
        }
        
        return endpoints;
    }
    
    private Map<String, Parameter> extractParameters(JsonNode operation) {
        Map<String, Parameter> parameters = new HashMap<>();
        JsonNode paramsNode = operation.get("parameters");
        
        if (paramsNode != null && paramsNode.isArray()) {
            for (JsonNode paramNode : paramsNode) {
                String name = paramNode.path("name").asText();
                String in = paramNode.path("in").asText("query");
                boolean required = paramNode.path("required").asBoolean(false);
                String type = paramNode.path("schema").path("type").asText("string");
                
                Parameter param = new Parameter(name, ParameterType.valueOf(in.toUpperCase()), 
                                              required, type, "");
                parameters.put(name, param);
            }
        }
        
        return parameters;
    }
    
    private List<String> extractSecurityRequirements(JsonNode operation) {
        List<String> requirements = new ArrayList<>();
        JsonNode securityNode = operation.get("security");
        
        if (securityNode != null && securityNode.isArray()) {
            for (JsonNode secReq : securityNode) {
                secReq.fieldNames().forEachRemaining(requirements::add);
            }
        }
        
        return requirements;
    }
    
    private List<SecurityScheme> extractSecuritySchemes(JsonNode root) {
        List<SecurityScheme> schemes = new ArrayList<>();
        JsonNode components = root.get("components");
        
        if (components != null) {
            JsonNode securitySchemes = components.get("securitySchemes");
            if (securitySchemes != null) {
                securitySchemes.fields().forEachRemaining(schemeEntry -> {
                    String name = schemeEntry.getKey();
                    JsonNode scheme = schemeEntry.getValue();
                    String type = scheme.path("type").asText();
                    String in = scheme.path("in").asText("");
                    String schemeName = scheme.path("scheme").asText("");
                    
                    schemes.add(new SecurityScheme(name, SecurityType.valueOf(type.toUpperCase()), 
                                                 in, schemeName));
                });
            }
        }
        
        return schemes;
    }
    
    private String extractSchema(JsonNode operation, String field) {
        JsonNode fieldNode = operation.get(field);
        return fieldNode != null ? fieldNode.toString() : "";
    }
    
    private boolean isHttpMethod(String method) {
        try {
            HttpMethod.valueOf(method.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}