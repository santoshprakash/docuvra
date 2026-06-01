package com.docuvra.exception;

import java.util.UUID;

public class MaxVersionLimitException extends RuntimeException {

    public MaxVersionLimitException(UUID documentId) {
        super("This document already has the maximum allowed 5 versions: " + documentId);
    }
}

