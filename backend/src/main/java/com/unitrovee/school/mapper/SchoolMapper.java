package com.unitrovee.school.mapper;

import com.unitrovee.school.domain.School;
import com.unitrovee.school.dto.SchoolResponse;
import org.springframework.stereotype.Component;

/**
 * converts School entities into SchoolResponse DTOs
 */
@Component
public class SchoolMapper {
    // Entity -> DTO.
    public SchoolResponse toResponse(School school) {
        return new SchoolResponse(
                school.getId(),
                school.getName(),
                school.getShortName(),
                school.getEmailDomain(),
                school.getCity()
        );
    }
}
