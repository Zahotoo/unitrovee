package com.unitrovee.common.exception;

/**
 * Domain exception meaning "the requested resource does not exist".
 * Services throw this: GlobalExceptionHandler maps it to HTTP 404 -> ErrorResponse
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
