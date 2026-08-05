package com.unitrovee.auth;

import com.unitrovee.auth.dto.LoginRequest;
import com.unitrovee.auth.dto.LoginResponse;
import com.unitrovee.auth.dto.RegisterRequest;
import com.unitrovee.auth.dto.RegisterResponse;
import com.unitrovee.auth.dto.VerifyEmailRequest;
import com.unitrovee.auth.dto.VerifyEmailResponse;
import com.unitrovee.auth.dto.ResendVerificationRequest;
import com.unitrovee.auth.dto.ResendVerificationResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    VerifyEmailResponse verifyEmail(VerifyEmailRequest request);

    ResendVerificationResponse resendVerificationCode(ResendVerificationRequest request);
}