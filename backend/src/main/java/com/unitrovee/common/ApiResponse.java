package com.unitrovee.common;

/**
 * Standard success envelope for all API responses
 */
public record ApiResponse<T>(T data, String message) {

    /**
     * Factory for the common case
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data, "Success");
    }

    /**
     * Factory when the caller wants a custom message
     */
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(data, message);
    }
}