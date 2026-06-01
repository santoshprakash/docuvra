package com.docuvra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "docuvra.storage")
public record StorageProperties(
        String basePath,
        String convertedPath
) {
}
