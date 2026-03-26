package com.renote.backend.service;

import com.renote.backend.dto.CurrentUserResponse;
import com.renote.backend.dto.LoginRequest;
import com.renote.backend.dto.LoginResponse;
import com.renote.backend.dto.RegisterRequest;

public interface AuthService {

    LoginResponse register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    CurrentUserResponse getCurrentUser(Long userId);
}
