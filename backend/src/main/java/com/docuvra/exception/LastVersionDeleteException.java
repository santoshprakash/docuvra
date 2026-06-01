package com.docuvra.exception;

import java.util.UUID;

public class LastVersionDeleteException extends RuntimeException {

    public LastVersionDeleteException(UUID documentId) {
        super("Cannot delete the last remaining version of document: " + documentId);
    }
}

