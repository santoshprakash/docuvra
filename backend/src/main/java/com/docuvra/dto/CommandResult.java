package com.docuvra.dto;

public record CommandResult(
        int exitCode,
        String stdout,
        String stderr,
        boolean success
) {
}
