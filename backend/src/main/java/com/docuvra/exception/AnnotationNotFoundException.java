package com.docuvra.exception;

import java.util.UUID;

public class AnnotationNotFoundException extends RuntimeException {

    public AnnotationNotFoundException(UUID annotationId) {
        super("Annotation not found: " + annotationId);
    }
}
