package com.unitrovee.auth;

import com.unitrovee.auth.dto.RegisterRequest;
import com.unitrovee.auth.dto.RegisterResponse;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);
}