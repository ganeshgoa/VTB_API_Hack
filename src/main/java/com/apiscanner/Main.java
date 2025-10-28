package com.apiscanner;

import com.apiscanner.cli.CliArgs;
import com.apiscanner.cli.CliParser;
import com.apiscanner.core.AppContainer;
import com.apiscanner.core.SpecLoader;
import com.apiscanner.domain.ApiSpec;
import com.apiscanner.exceptions.SpecLoadException;
import com.apiscanner.loaders.OpenApiSpecLoader;

import java.nio.file.Files;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            System.out.println("🔍 API Security Scanner starting...");
            
            // Parse CLI arguments
            CliArgs cliArgs = CliParser.parse(args);
            
            if (cliArgs.verbose()) {
                System.out.println("📁 Output directory: " + cliArgs.outputDir());
                System.out.println("📊 Report format: " + cliArgs.format());
            }
            
            // Load API specification
            ApiSpec apiSpec = loadApiSpec(cliArgs);
            
            System.out.println("✅ Successfully loaded API specification:");
            System.out.println("   Title: " + apiSpec.title());
            System.out.println("   Version: " + apiSpec.version());
            System.out.println("   Endpoints: " + countEndpoints(apiSpec));
            System.out.println("   Base URL: " + apiSpec.baseUrl());
            
            // Create output directory
            Files.createDirectories(cliArgs.outputDir());
            
            System.out.println("🚀 Ready for security scanning...");
            // Здесь будет интеграция с следующими модулями
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            if (args.length > 0 && (args[0].equals("-v") || args[0].equals("--verbose"))) {
                e.printStackTrace();
            }
            System.exit(1);
        }
    }
    
    private static ApiSpec loadApiSpec(CliArgs cliArgs) throws SpecLoadException {
        SpecLoader loader = new OpenApiSpecLoader();
        
        if (cliArgs.hasSpecFile()) {
            if (cliArgs.verbose()) {
                System.out.println("📄 Loading spec from file: " + cliArgs.specFile());
            }
            return loader.loadFromFile(cliArgs.specFile());
        } else if (cliArgs.hasTargetUrl()) {
            if (cliArgs.verbose()) {
                System.out.println("🌐 Loading spec from URL: " + cliArgs.targetUrl());
            }
            return loader.loadFromUrl(cliArgs.targetUrl());
        } else {
            throw new IllegalStateException("No spec source provided");
        }
    }
    
    private static int countEndpoints(ApiSpec apiSpec) {
        return apiSpec.endpoints().values().stream()
            .mapToInt(List::size)
            .sum();
    }
}