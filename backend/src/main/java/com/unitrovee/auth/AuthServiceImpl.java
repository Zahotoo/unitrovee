package com.unitrovee.auth;

import com.unitrovee.auth.dto.*;
import com.unitrovee.auth.exception.EmailAlreadyExistsException;
import com.unitrovee.auth.exception.UnsupportedSchoolEmailException;
import com.unitrovee.auth.mapper.AuthMapper;
import com.unitrovee.school.SchoolRepository;
import com.unitrovee.school.domain.School;
import com.unitrovee.security.JwtService;
import com.unitrovee.user.UserRepository;
import com.unitrovee.user.domain.Role;
import com.unitrovee.user.domain.User;
import com.unitrovee.auth.exception.InvalidVerificationCodeException;
import com.unitrovee.auth.verification.EmailVerificationCode;
import com.unitrovee.auth.verification.EmailVerificationCodeRepository;
import com.unitrovee.auth.verification.VerificationCodeHasher;
import com.unitrovee.auth.verification.VerificationCodeGenerator;
import com.unitrovee.auth.verification.VerificationEmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthMapper authMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailVerificationCodeRepository verificationCodeRepository;
    private final VerificationCodeGenerator verificationCodeGenerator;
    private final VerificationEmailSender verificationEmailSender;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

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

        String plainCode = verificationCodeGenerator.generate();

        EmailVerificationCode verificationCode = new EmailVerificationCode();
        verificationCode.setUser(savedUser);
        verificationCode.setCodeHash(VerificationCodeHasher.hash(plainCode));
        verificationCode.setExpiresAt(Instant.now().plus(Duration.ofMinutes(15)));

        verificationCodeRepository.save(verificationCode);
        verificationEmailSender.sendVerificationCode(savedUser.getEmail(), plainCode);

        return authMapper.toRegisterResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        // spring loads the user and compares the raw password with the BCrypt hash
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String accessToken = jwtService.generateToken(userDetails);

        return new LoginResponse(accessToken);
    }

    @Override
    @Transactional
    public VerifyEmailResponse verifyEmail(VerifyEmailRequest request) {
        String                  email               = request.email().trim().toLowerCase(Locale.ROOT);
        User                    user                = userRepository.findByEmailForUpdate(email).orElseThrow(() -> new InvalidVerificationCodeException("Verification code is invalid or expired"));
        EmailVerificationCode   verificationCode    = verificationCodeRepository.findByUserId(user.getId()).orElseThrow(() -> new InvalidVerificationCodeException("Verification code is invalid or expired"));
        boolean                 isExpired           = !verificationCode.getExpiresAt().isAfter(Instant.now());
        String                  requestCodeHash     = VerificationCodeHasher.hash(request.code());

        // constant-time comparison avoids leaking information through timing
        boolean codeMatches = MessageDigest.isEqual(
                verificationCode.getCodeHash().getBytes(StandardCharsets.UTF_8),
                requestCodeHash.getBytes(StandardCharsets.UTF_8)
        );

        if (isExpired || !codeMatches) {
            throw new InvalidVerificationCodeException("Verification code is invalid or expired");
        }

        user.setEmailVerified(true);
        verificationCodeRepository.delete(verificationCode);

        return new VerifyEmailResponse(user.getEmail(), true);
    }

    @Override
    @Transactional
    public ResendVerificationResponse resendVerificationCode(ResendVerificationRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailForUpdate(email).orElse(null);

        if (user == null || user.isEmailVerified()) {
            return new ResendVerificationResponse(RESEND_COOLDOWN_SECONDS);
        }

        EmailVerificationCode verificationCode = verificationCodeRepository.findByUserId(user.getId()).orElse(null);

        if (verificationCode != null) {
            Instant cooldownEndsAt = verificationCode.getUpdatedAt().plusSeconds(RESEND_COOLDOWN_SECONDS);
            if (cooldownEndsAt.isAfter(Instant.now())) {
                // returns the same generic response without sending another email
                // this prevents email/account-state enumeration
                return new ResendVerificationResponse(RESEND_COOLDOWN_SECONDS);
            }
        } else {
            verificationCode = new EmailVerificationCode();
            verificationCode.setUser(user);
        }

        String plainCode = verificationCodeGenerator.generate();

        verificationCode.setCodeHash(VerificationCodeHasher.hash(plainCode));
        verificationCode.setExpiresAt(Instant.now().plus(Duration.ofMinutes(15)));
        verificationCodeRepository.save(verificationCode);
        verificationEmailSender.sendVerificationCode(user.getEmail(), plainCode);
        return new ResendVerificationResponse(RESEND_COOLDOWN_SECONDS);
    }
}
