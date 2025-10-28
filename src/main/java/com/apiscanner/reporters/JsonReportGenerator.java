package com.apiscanner.reporters;

import com.apiscanner.core.ReportGenerator;
import com.apiscanner.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonReportGenerator implements ReportGenerator {
    private final ObjectMapper mapper;
    
    public JsonReportGenerator() {
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    
    @Override
    public void generateReport(ApiSpec apiSpec, Path outputPath) {
        try {
            Map<String, Object> report = new HashMap<>();
            report.put("apiTitle", apiSpec.title());
            report.put("apiVersion", apiSpec.version());
            report.put("baseUrl", apiSpec.baseUrl());
            report.put("endpoints", generateEndpointsReport(apiSpec.endpoints()));
            
            String jsonContent = mapper.writeValueAsString(report);
            Path reportFile = outputPath.resolve("api-schema-report.json");
            Files.writeString(reportFile, jsonContent);
            
            System.out.println("📊 JSON schema report generated: " + reportFile.toAbsolutePath());
            
        } catch (Exception e) {
            System.err.println("❌ Failed to generate JSON report: " + e.getMessage());
        }
    }
    
    private List<Map<String, Object>> generateEndpointsReport(Map<String, List<Endpoint>> endpoints) {
        List<Map<String, Object>> endpointsReport = new ArrayList<>();
        
        for (var pathEntry : endpoints.entrySet()) {
            String path = pathEntry.getKey();
            for (Endpoint endpoint : pathEntry.getValue()) {
                Map<String, Object> endpointReport = new HashMap<>();
                endpointReport.put("path", path);
                endpointReport.put("method", endpoint.method().name());
                endpointReport.put("operationId", endpoint.operationId());
                endpointReport.put("summary", endpoint.summary());
                endpointReport.put("description", endpoint.description());
                
                // Input Schema (Request Body)
                if (endpoint.requestBody() != null) {
                    endpointReport.put("inputSchema", convertSchemaToMap(endpoint.requestBody().schema()));
                } else {
                    endpointReport.put("inputSchema", null);
                }
                
                // Output Schemas (Responses)
                List<Map<String, Object>> outputSchemas = new ArrayList<>();
                for (var responseEntry : endpoint.responses().entrySet()) {
                    Map<String, Object> responseSchema = new HashMap<>();
                    responseSchema.put("statusCode", responseEntry.getKey());
                    responseSchema.put("description", responseEntry.getValue().description());
                    responseSchema.put("schema", convertSchemaToMap(responseEntry.getValue().schema()));
                    outputSchemas.add(responseSchema);
                }
                endpointReport.put("outputSchemas", outputSchemas);
                
                // Parameters
                endpointReport.put("parameters", endpoint.parameters());
                
                endpointsReport.add(endpointReport);
            }
        }
        
        return endpointsReport;
    }
    
    private Map<String, Object> convertSchemaToMap(Schema schema) {
        if (schema == null) {
            return null;
        }
        
        Map<String, Object> schemaMap = new HashMap<>();
        schemaMap.put("type", schema.type());
        schemaMap.put("format", schema.format());
        schemaMap.put("description", schema.description());
        
        if (schema.properties() != null && !schema.properties().isEmpty()) {
            Map<String, Object> propertiesMap = new HashMap<>();
            for (var propEntry : schema.properties().entrySet()) {
                propertiesMap.put(propEntry.getKey(), convertSchemaToMap(propEntry.getValue()));
            }
            schemaMap.put("properties", propertiesMap);
        }
        
        if (schema.items() != null) {
            schemaMap.put("items", convertSchemaToMap(schema.items()));
        }
        
        return schemaMap;
    }
}