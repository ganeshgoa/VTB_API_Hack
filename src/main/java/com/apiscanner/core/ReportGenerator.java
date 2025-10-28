package com.apiscanner.core;

import com.apiscanner.domain.ApiSpec;
import java.nio.file.Path;

public interface ReportGenerator {
    void generateReport(ApiSpec apiSpec, Path outputPath);
}