// cli/CliArgs.java
package com.apiscanner.cli;

import java.nio.file.Path;

public record CliArgs(
    Path specFile,
    String targetUrl,
    Path outputDir,
    String format,
    boolean verbose,
    String configFile
) {
    public CliArgs {
        // Default values
        if (outputDir == null) {
            outputDir = Path.of("./reports");
        }
        if (format == null) {
            format = "html";
        }
        if (configFile == null) {
            configFile = "config.properties";
        }
    }
    
    public boolean hasSpecFile() {
        return specFile != null;
    }
    
    public boolean hasTargetUrl() {
        return targetUrl != null && !targetUrl.isBlank();
    }
    
    public void validate() {
        if (!hasSpecFile() && !hasTargetUrl()) {
            throw new IllegalArgumentException(
                "Either --spec or --target must be provided"
            );
        }
    }
}