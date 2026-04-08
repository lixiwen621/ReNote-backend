package com.renote.backend.service.impl;

import com.renote.backend.common.I18nPreconditions;
import com.renote.backend.dto.CurrentUserResponse;
import com.renote.backend.dto.LoginRequest;
import com.renote.backend.dto.LoginResponse;
import com.renote.backend.dto.RegisterRequest;
import com.renote.backend.entity.User;
import com.renote.backend.enums.UserStatus;
import com.renote.backend.mapper.UserMapper;
import com.renote.backend.security.JwtTokenProvider;
import com.renote.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.expiration-ms:86400000}")
    private long expirationMs;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse register(RegisterRequest request) {
        I18nPreconditions.checkNotNull(request, "error.auth.registerRequest.required");
        I18nPreconditions.checkState(userMapper.findByUsername(request.getUsername().trim()) == null,
                "error.auth.usernameDuplicate");
        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE.code());
        userMapper.insert(user);
        return buildLoginResponse(user.getId());
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        I18nPreconditions.checkNotNull(request, "error.auth.loginRequest.required");
        User user = userMapper.findByUsername(request.getUsername().trim());
        I18nPreconditions.checkNotNull(user, "error.auth.usernameNotFound");
        I18nPreconditions.checkArgument(passwordEncoder.matches(request.getPassword(), user.getPasswordHash()),
                "error.auth.passwordIncorrect");
        I18nPreconditions.checkState(Integer.valueOf(UserStatus.ACTIVE.code()).equals(user.getStatus()),
                "error.auth.accountDisabled");
        return buildLoginResponse(user.getId());
    }

    @Override
    public CurrentUserResponse getCurrentUser(Long userId) {
        User user = userMapper.findById(userId);
        I18nPreconditions.checkNotNull(user, "error.auth.userNotFound", userId);
        return CurrentUserResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .build();
    }

    private LoginResponse buildLoginResponse(Long userId) {
        String token = jwtTokenProvider.createToken(userId);
        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresInMs(expirationMs)
                .userId(userId)
                .build();
    }
}
