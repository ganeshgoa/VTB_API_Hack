// core/SpecLoader.java
package com.apiscanner.core;

import com.apiscanner.domain.ApiSpec;
import com.apiscanner.exceptions.SpecLoadException;
import java.nio.file.Path;

public interface SpecLoader {
    ApiSpec loadFromFile(Path filePath) throws SpecLoadException;
    ApiSpec loadFromUrl(String url) throws SpecLoadException;
    boolean supports(String contentType);
}