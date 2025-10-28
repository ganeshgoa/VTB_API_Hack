// core/AppContainer.java
package com.apiscanner.core;

import com.apiscanner.loaders.OpenApiSpecLoader;
import java.util.List;

public class AppContainer {
    
    public static List<SpecLoader> createSpecLoaders() {
        return List.of(
            new OpenApiSpecLoader()
            // Можно добавить другие загрузчики позже
            // new AsyncApiLoader(),
            // new GraphQLSpecLoader()
        );
    }
    
    public static SpecLoader findSupportedLoader(String contentType, List<SpecLoader> loaders) {
        return loaders.stream()
            .filter(loader -> loader.supports(contentType))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "No supported loader for content type: " + contentType
            ));
    }
}