package com.unitrovee.auth.dto;

public record ResendVerificationResponse(
        int retryAfterSeconds
) {}
