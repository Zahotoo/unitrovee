package com.unitrovee.auth.dto;

import com.unitrovee.school.dto.SchoolResponse;

public record RegisterResponse (
        Long id,
        String email,
        String displayName,
        boolean emailVerified,
        SchoolResponse school
) {}
