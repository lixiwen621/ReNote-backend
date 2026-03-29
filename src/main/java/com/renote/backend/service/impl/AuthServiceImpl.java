package com.renote.backend.service.impl;

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
        if (userMapper.findByUsername(request.getUsername().trim()) != null) {
            throw new IllegalArgumentException("该用户名已经注册请换一个用户名");
        }
        User user = new User();
        user.setUsername(request.getUsername().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE.code());
        userMapper.insert(user);
        return buildLoginResponse(user.getId());
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userMapper.findByUsername(request.getUsername().trim());
        if (user == null) {
            throw new IllegalArgumentException("没有该用户名，请注册一下");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("密码不正确");
        }
        if (!Integer.valueOf(UserStatus.ACTIVE.code()).equals(user.getStatus())) {
            throw new IllegalArgumentException("账号已禁用");
        }
        return buildLoginResponse(user.getId());
    }

    @Override
    public CurrentUserResponse getCurrentUser(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在: " + userId);
        }
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
