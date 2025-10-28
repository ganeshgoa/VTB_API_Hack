// cli/CliParser.java
package com.apiscanner.cli;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CliParser {
    
    public static CliArgs parse(String[] args) {
        Path specFile = null;
        String targetUrl = null;
        Path outputDir = null;
        String format = null;
        boolean verbose = false;
        String configFile = null;
        
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--spec", "-s" -> {
                        if (i + 1 >= args.length) {
                            throw new IllegalArgumentException("Missing value for --spec");
                        }
                        specFile = Path.of(args[++i]);
                    }
                    case "--target", "-t" -> {
                        if (i + 1 >= args.length) {
                            throw new IllegalArgumentException("Missing value for --target");
                        }
                        targetUrl = args[++i];
                    }
                    case "--output", "-o" -> {
                        if (i + 1 >= args.length) {
                            throw new IllegalArgumentException("Missing value for --output");
                        }
                        outputDir = Path.of(args[++i]);
                    }
                    case "--format", "-f" -> {
                        if (i + 1 >= args.length) {
                            throw new IllegalArgumentException("Missing value for --format");
                        }
                        format = args[++i];
                    }
                    case "--verbose", "-v" -> {
                        verbose = true;
                    }
                    case "--config", "-c" -> {
                        if (i + 1 >= args.length) {
                            throw new IllegalArgumentException("Missing value for --config");
                        }
                        configFile = args[++i];
                    }
                    case "--help", "-h" -> {
                        printHelp();
                        System.exit(0);
                    }
                    default -> throw new IllegalArgumentException("Unknown option: " + args[i]);
                }
            }
            
            CliArgs cliArgs = new CliArgs(specFile, targetUrl, outputDir, format, verbose, configFile);
            cliArgs.validate();
            return cliArgs;
            
        } catch (Exception e) {
            System.err.println("Error parsing arguments: " + e.getMessage());
            printHelp();
            System.exit(1);
            return null; // unreachable
        }
    }
    
    private static void printHelp() {
        System.out.println("""
            API Security Scanner - OWASP API Top 10 Vulnerability Scanner
            
            Usage: java -jar apiscan.jar [OPTIONS]
            
            OPTIONS:
              -s, --spec FILE      OpenAPI/Swagger specification file (YAML/JSON)
              -t, --target URL     Target API URL for live scanning
              -o, --output DIR     Output directory for reports (default: ./reports)
              -f, --format FORMAT  Report format: html, json, pdf (default: html)
              -c, --config FILE    Configuration file (default: config.properties)
              -v, --verbose        Enable verbose logging
              -h, --help           Show this help message
            
            EXAMPLES:
              # Scan using OpenAPI spec
              java -jar apiscan.jar -s openapi.yaml -o ./scan-results
              
              # Scan live API
              java -jar apiscan.jar -t https://api.example.com -f json
              
              # Combined scan with verbose output
              java -jar apiscan.jar -s openapi.yaml -t https://api.example.com -v
            """);
    }
}