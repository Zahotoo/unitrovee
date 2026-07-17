package com.unitrovee.auth;

import com.unitrovee.auth.dto.LoginRequest;
import com.unitrovee.auth.dto.LoginResponse;
import com.unitrovee.auth.dto.RegisterRequest;
import com.unitrovee.auth.dto.RegisterResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);
}