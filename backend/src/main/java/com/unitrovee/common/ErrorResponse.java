package com.unitrovee.common;

/**
 * Standard error envelope: { "error": { "code": ..., "message": ... } }
 */
public record ErrorResponse(ErrorDetail error) {

    // Inner payload: the actual { "code": ..., "message": ... } object.
    // A record declared inside another record is implicitly static — it just
    // groups this related type in the same file.
    public record ErrorDetail(String code, String message) {}

    // Factory: build the whole envelope from a code + message in one call.
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new ErrorDetail(code, message));
    }
}