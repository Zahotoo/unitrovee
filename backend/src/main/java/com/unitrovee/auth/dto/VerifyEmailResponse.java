package com.unitrovee.auth.dto;

public record VerifyEmailResponse(
        String email,
        boolean emailVerified
) {}
