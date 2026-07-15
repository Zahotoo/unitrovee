package com.unitrovee.auth.mapper;

import com.unitrovee.auth.dto.RegisterResponse;
import com.unitrovee.school.mapper.SchoolMapper;
import com.unitrovee.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthMapper {

    private final SchoolMapper schoolMapper;

    public RegisterResponse toRegisterResponse(User user) {
        return new RegisterResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.isEmailVerified(),
                schoolMapper.toResponse(user.getSchool())
        );
    }
}
