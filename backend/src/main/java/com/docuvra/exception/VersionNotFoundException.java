package com.docuvra.exception;

import java.util.UUID;

public class VersionNotFoundException extends RuntimeException {

    public VersionNotFoundException(UUID documentId, UUID versionId) {
        super("Version not found for document " + documentId + ": " + versionId);
    }
}

