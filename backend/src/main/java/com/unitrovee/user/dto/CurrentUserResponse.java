package com.unitrovee.user.dto;

import com.unitrovee.user.domain.Role;

public record CurrentUserResponse(
        Long id,
        String email,
        String displayName,
        Role role,
        boolean emailVerified,
        int reputationScore,
        SchoolSummaryResponse school
) {}
