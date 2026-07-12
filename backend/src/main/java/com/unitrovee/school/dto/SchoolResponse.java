package com.unitrovee.school.dto;

/**
 * public API shape of a school. Controllers/Services return THIS
 */
public record SchoolResponse(
        Long id,
        String name,
        String shortName,
        String emailDomain,
        String city
) {}
