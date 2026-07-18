package com.unitrovee.user.mapper;

import com.unitrovee.user.domain.User;
import com.unitrovee.user.dto.CurrentUserResponse;
import com.unitrovee.user.dto.SchoolSummaryResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public CurrentUserResponse toCurrentUserResponse(User user) {
        return new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.isEmailVerified(),
                user.getReputationScore(),
                new SchoolSummaryResponse(
                        user.getSchool().getId(),
                        user.getSchool().getName()
                )
        );
    }
}
