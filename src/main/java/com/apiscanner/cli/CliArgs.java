package com.apiscanner.cli;

import java.nio.file.Path;

public record CliArgs(
    Path specFile,
    String targetUrl,
    boolean verbose
) {
    public CliArgs {
        // Default values
    }
    
    public boolean hasSpecFile() {
        return specFile != null;
    }
    
    public boolean hasTargetUrl() {
        return targetUrl != null && !targetUrl.isBlank();
    }
    
    public void validate() {
        if (!hasSpecFile() && !hasTargetUrl()) {
            throw new IllegalArgumentException("Either --spec or --target must be provided");
        }
        if (hasSpecFile() && hasTargetUrl()) {
            throw new IllegalArgumentException("Use either --spec or --target, not both");
        }
    }
}