package com.apiscanner;

import com.apiscanner.cli.CliArgs;
import com.apiscanner.cli.CliParser;
import com.apiscanner.core.ReportGenerator;
import com.apiscanner.core.SpecLoader;
import com.apiscanner.domain.ApiSpec;
import com.apiscanner.exceptions.SpecLoadException;
import com.apiscanner.loaders.OpenApiSpecLoader;
import com.apiscanner.reporters.JsonReportGenerator;

import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        try {
            System.out.println("🔍 API Security Scanner starting...");
            
            // Parse CLI arguments
            CliArgs cliArgs = CliParser.parse(args);
            
            if (cliArgs.verbose()) {
                System.out.println("🔧 Verbose mode enabled");
            }
            
            // Load API specification
            ApiSpec apiSpec = loadApiSpec(cliArgs);
            
            // Create output directory
            Path outputDir = Path.of("./reports");
            Files.createDirectories(outputDir);
            
            // Generate JSON schema report
            System.out.println("\n📊 Generating schema report...");
            ReportGenerator reportGenerator = new JsonReportGenerator();
            reportGenerator.generateReport(apiSpec, outputDir);
            
            System.out.println("✅ API schema analysis completed!");
            
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
                System.out.println("📄 Loading spec from file: " + cliArgs.specFile().toAbsolutePath());
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
}