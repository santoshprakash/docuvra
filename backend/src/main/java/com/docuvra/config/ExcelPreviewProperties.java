package com.docuvra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "docuvra.excel")
public record ExcelPreviewProperties(
        int maxRows,
        int maxColumns
) {
}
