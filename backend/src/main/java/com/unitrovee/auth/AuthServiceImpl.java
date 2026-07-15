package com.unitrovee.auth;

import com.unitrovee.auth.dto.RegisterRequest;
import com.unitrovee.auth.dto.RegisterResponse;
import com.unitrovee.auth.mapper.AuthMapper;
import com.unitrovee.school.SchoolRepository;
import com.unitrovee.school.domain.School;
import com.unitrovee.user.UserRepository;
import com.unitrovee.user.domain.Role;
import com.unitrovee.user.domain.User;
import com.unitrovee.auth.exception.EmailAlreadyExistsException;
import com.unitrovee.auth.exception.UnsupportedSchoolEmailException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthMapper authMapper;

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email is already registered");
        }

        String emailDomain = email.substring(email.lastIndexOf('@') + 1);

        School school = schoolRepository.findByEmailDomain(emailDomain)
                .filter(School::isActive)
                .orElseThrow(() -> new UnsupportedSchoolEmailException(
                        "Email domain is not supported by unitrovee"
                ));

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName().trim());
        user.setSchool(school);

        // Server-owned fields: client never chooses these values
        user.setRole(Role.STUDENT);
        user.setEmailVerified(false);
        user.setReputationScore(0);

        User savedUser = userRepository.save(user);
        return authMapper.toRegisterResponse(savedUser);
    }
}
