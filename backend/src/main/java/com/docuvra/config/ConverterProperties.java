package com.docuvra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "docuvra.converter")
public record ConverterProperties(
        String officePath,
        String imagemagickPath,
        long timeoutSeconds
) {
}
