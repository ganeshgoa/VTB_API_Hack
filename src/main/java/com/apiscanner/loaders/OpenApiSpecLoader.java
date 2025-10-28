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
            System.out.println("📄 Reading file: " + filePath.toAbsolutePath());
            
            if (!Files.exists(filePath)) {
                throw new SpecLoadException("File does not exist: " + filePath);
            }
            
            String content = Files.readString(filePath);
            
            if (content == null || content.trim().isEmpty()) {
                throw new SpecLoadException("File is empty: " + filePath);
            }
            
            System.out.println("✅ File read successfully, size: " + content.length() + " bytes");
            return parseSpecContent(content, filePath.toString());
            
        } catch (SpecLoadException e) {
            throw e;
        } catch (Exception e) {
            throw new SpecLoadException("Failed to load spec from file: " + filePath + " - " + e.getMessage(), e);
        }
    }
    
    @Override
    public ApiSpec loadFromUrl(String url) throws SpecLoadException {
        try {
            System.out.println("🌐 Fetching from URL: " + url);
            
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
                System.out.println("   Using URL: " + url);
            }
            
            java.net.URL specUrl = new java.net.URL(url);
            try (java.util.Scanner scanner = new java.util.Scanner(specUrl.openStream())) {
                String content = scanner.useDelimiter("\\A").next();
                
                if (content == null || content.trim().isEmpty()) {
                    throw new SpecLoadException("Empty content from URL");
                }
                
                System.out.println("✅ URL fetched successfully, size: " + content.length() + " bytes");
                return parseSpecContent(content, url);
            }
            
        } catch (SpecLoadException e) {
            throw e;
        } catch (Exception e) {
            throw new SpecLoadException("Failed to load spec from URL: " + url + " - " + e.getMessage(), e);
        }
    }
    
    private ApiSpec parseSpecContent(String content, String source) throws Exception {
        System.out.println("🔍 Parsing spec content...");
        
        JsonNode root;
        try {
            root = jsonMapper.readTree(content);
            System.out.println("✅ Parsed as JSON");
        } catch (Exception jsonError) {
            try {
                root = yamlMapper.readTree(content);
                System.out.println("✅ Parsed as YAML");
            } catch (Exception yamlError) {
                throw new SpecLoadException("Content is neither valid JSON nor YAML: " + source);
            }
        }
        
        if (!root.has("openapi") && !root.has("swagger")) {
            throw new SpecLoadException("Not a valid OpenAPI specification: " + source);
        }
        
        String openapiVersion = root.has("openapi") ? 
            root.get("openapi").asText() : 
            root.get("swagger").asText();
        System.out.println("📋 OpenAPI version: " + openapiVersion);
        
        String baseUrl = extractBaseUrl(root);
        String title = root.path("info").path("title").asText("Untitled API");
        String version = root.path("info").path("version").asText("1.0.0");
        
        Map<String, List<Endpoint>> endpoints = extractEndpoints(root);
        
        System.out.println("📊 Parsed API Details:");
        System.out.println("   - Title: " + title);
        System.out.println("   - Version: " + version);
        System.out.println("   - Base URL: " + baseUrl);
        System.out.println("   - Total Endpoints: " + countTotalEndpoints(endpoints));
        
        return new ApiSpec(baseUrl, title, version, endpoints);
    }
    
    private String extractBaseUrl(JsonNode root) {
        JsonNode servers = root.get("servers");
        if (servers != null && servers.isArray() && servers.size() > 0) {
            return servers.get(0).path("url").asText("");
        }
        
        JsonNode host = root.get("host");
        if (host != null) {
            String scheme = root.has("schemes") ? root.get("schemes").get(0).asText("https") : "https";
            String basePath = root.path("basePath").asText("");
            return scheme + "://" + host.asText() + basePath;
        }
        
        return "";
    }
    
    private Map<String, List<Endpoint>> extractEndpoints(JsonNode root) {
        Map<String, List<Endpoint>> endpoints = new LinkedHashMap<>();
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
                        
                        Endpoint endpoint = extractEndpointDetails(path, method, operation);
                        pathEndpoints.add(endpoint);
                    }
                });
                
                if (!pathEndpoints.isEmpty()) {
                    endpoints.put(path, pathEndpoints);
                }
            });
        }
        
        return endpoints;
    }
    
    private Endpoint extractEndpointDetails(String path, HttpMethod method, JsonNode operation) {
        String operationId = operation.path("operationId").asText("");
        String summary = operation.path("summary").asText("");
        String description = operation.path("description").asText("");
        
        List<Parameter> parameters = extractParameters(operation);
        RequestBody requestBody = extractRequestBody(operation);
        Map<String, Response> responses = extractResponses(operation);
        List<String> securityRequirements = extractSecurityRequirements(operation);
        List<String> tags = extractTags(operation);
        
        return new Endpoint(
            path, method, operationId, summary, description,
            parameters, requestBody, responses, securityRequirements, tags
        );
    }
    
    private List<Parameter> extractParameters(JsonNode operation) {
    List<Parameter> parameters = new ArrayList<>();
    JsonNode paramsNode = operation.get("parameters");
    
    if (paramsNode != null && paramsNode.isArray()) {
        for (JsonNode paramNode : paramsNode) {
            String name = paramNode.path("name").asText();
            String in = paramNode.path("in").asText("query");
            boolean required = paramNode.path("required").asBoolean(false);
            String paramDescription = paramNode.path("description").asText("");
            
            String dataType = "string";
            String format = "";
            
            JsonNode schemaNode = paramNode.get("schema");
            if (schemaNode != null) {
                dataType = schemaNode.path("type").asText("string");
                format = schemaNode.path("format").asText("");
            }
            
            Parameter parameter = new Parameter(name, in, required, dataType, format, paramDescription);
            parameters.add(parameter);
        }
    }
    
    return parameters;
}
    
    private RequestBody extractRequestBody(JsonNode operation) {
        JsonNode requestBody = operation.get("requestBody");
        if (requestBody == null) {
            return null;
        }
        
        JsonNode content = requestBody.get("content");
        if (content != null && content.has("application/json")) {
            JsonNode schemaNode = content.get("application/json").get("schema");
            Schema schema = parseSchema(schemaNode);
            String description = requestBody.path("description").asText("");
            
            return new RequestBody("application/json", schema, description);
        }
        
        return null;
    }
    
    private Map<String, Response> extractResponses(JsonNode operation) {
        Map<String, Response> responses = new HashMap<>();
        JsonNode responsesNode = operation.get("responses");
        
        if (responsesNode != null) {
            responsesNode.fields().forEachRemaining(responseEntry -> {
                String statusCode = responseEntry.getKey();
                JsonNode responseNode = responseEntry.getValue();
                String description = responseNode.path("description").asText("");
                
                Schema schema = null;
                JsonNode content = responseNode.get("content");
                if (content != null && content.has("application/json")) {
                    JsonNode schemaNode = content.get("application/json").get("schema");
                    schema = parseSchema(schemaNode);
                }
                
                responses.put(statusCode, new Response(statusCode, description, schema));
            });
        }
        
        return responses;
    }
    
    private Schema parseSchema(JsonNode schemaNode) {
        if (schemaNode == null) {
            return null;
        }
        
        String type = schemaNode.path("type").asText("");
        String format = schemaNode.path("format").asText("");
        String description = schemaNode.path("description").asText("");
        
        Map<String, Schema> properties = new HashMap<>();
        JsonNode propertiesNode = schemaNode.get("properties");
        if (propertiesNode != null) {
            propertiesNode.fields().forEachRemaining(propEntry -> {
                String propName = propEntry.getKey();
                JsonNode propSchema = propEntry.getValue();
                properties.put(propName, parseSchema(propSchema));
            });
        }
        
        Schema items = null;
        JsonNode itemsNode = schemaNode.get("items");
        if (itemsNode != null) {
            items = parseSchema(itemsNode);
        }
        
        return new Schema(type, properties, items, format, description, false);
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
    
    private List<String> extractTags(JsonNode operation) {
        List<String> tags = new ArrayList<>();
        JsonNode tagsNode = operation.get("tags");
        
        if (tagsNode != null && tagsNode.isArray()) {
            for (JsonNode tag : tagsNode) {
                tags.add(tag.asText());
            }
        }
        
        return tags;
    }
    
    private boolean isHttpMethod(String method) {
        try {
            HttpMethod.valueOf(method.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    private int countTotalEndpoints(Map<String, List<Endpoint>> endpoints) {
        return endpoints.values().stream().mapToInt(List::size).sum();
    }
}