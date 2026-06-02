package com.docuvra.exception;

import com.docuvra.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({DocumentNotFoundException.class, VersionNotFoundException.class, AnnotationNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuth(AuthException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "AUTH_ERROR", exception.getMessage(), request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "FORBIDDEN", exception.getMessage(), request);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleConflict(Exception exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "DUPLICATE_USER",
                "Username must be unique. Email and mobile must be unique for the selected role.", request);
    }

    @ExceptionHandler(MaxVersionLimitException.class)
    public ResponseEntity<ErrorResponse> handleMaxVersion(MaxVersionLimitException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "MAX_VERSION_LIMIT", exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFile(InvalidFileException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_FILE", exception.getMessage(), request);
    }

    @ExceptionHandler(LastVersionDeleteException.class)
    public ResponseEntity<ErrorResponse> handleLastVersionDelete(LastVersionDeleteException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "LAST_VERSION_DELETE", exception.getMessage(), request);
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ErrorResponse> handleStorageError(FileStorageException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_STORAGE_ERROR", exception.getMessage(), request);
    }

    @ExceptionHandler(UnsupportedConversionException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedConversion(
            UnsupportedConversionException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_CONVERSION", exception.getMessage(), request);
    }

    @ExceptionHandler(ConverterNotInstalledException.class)
    public ResponseEntity<ErrorResponse> handleConverterMissing(
            ConverterNotInstalledException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "CONVERTER_NOT_INSTALLED", exception.getMessage(), request);
    }

    @ExceptionHandler(ConversionException.class)
    public ResponseEntity<ErrorResponse> handleConversionError(ConversionException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "CONVERSION_FAILED", exception.getMessage(), request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONTENT_TOO_LARGE, "MAX_UPLOAD_SIZE_EXCEEDED",
                "File size must not exceed 50 MB.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred.", request);
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request
    ) {
        ErrorResponse response = new ErrorResponse(
                code,
                message,
                LocalDateTime.now(),
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(response);
    }
}
