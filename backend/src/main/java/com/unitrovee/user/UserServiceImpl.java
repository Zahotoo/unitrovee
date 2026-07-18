package com.unitrovee.user;

import com.unitrovee.common.exception.ResourceNotFoundException;
import com.unitrovee.user.domain.User;
import com.unitrovee.user.dto.CurrentUserResponse;
import com.unitrovee.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmailWithSchool(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return userMapper.toCurrentUserResponse(user);
    }
}
