package com.apiscanner.cli;

import java.nio.file.Path;

public class CliParser {
    
    public static CliArgs parse(String[] args) {
        Path specFile = null;
        String targetUrl = null;
        boolean verbose = false;
        
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--spec", "-s" -> {
                        if (i + 1 >= args.length) throw new IllegalArgumentException("Missing value for --spec");
                        specFile = Path.of(args[++i]);
                    }
                    case "--target", "-t" -> {
                        if (i + 1 >= args.length) throw new IllegalArgumentException("Missing value for --target");
                        targetUrl = args[++i];
                    }
                    case "--verbose", "-v" -> verbose = true;
                    case "--help", "-h" -> { printHelp(); System.exit(0); }
                    default -> throw new IllegalArgumentException("Unknown option: " + args[i]);
                }
            }
            
            CliArgs cliArgs = new CliArgs(specFile, targetUrl, verbose);
            cliArgs.validate();
            return cliArgs;
            
        } catch (Exception e) {
            System.err.println("Error parsing arguments: " + e.getMessage());
            printHelp();
            System.exit(1);
            return null;
        }
    }
    
    private static void printHelp() {
        System.out.println("""
            API Security Scanner
            Usage: java -jar apiscan.jar [OPTIONS]
            
            OPTIONS:
              -s, --spec FILE      OpenAPI specification file
              -t, --target URL     Target API URL for live scanning
              -v, --verbose        Enable verbose logging
              -h, --help           Show this help
            
            EXAMPLES:
              # Scan using OpenAPI spec file
              java -jar apiscan.jar -s openapi.yaml
              
              # Scan using API URL
              java -jar apiscan.jar -t https://api.example.com/openapi.json
              
              # Verbose mode
              java -jar apiscan.jar -t https://api.example.com/openapi.json -v
            """);
    }
}