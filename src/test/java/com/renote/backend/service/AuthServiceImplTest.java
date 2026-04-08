package com.renote.backend.service;

import com.renote.backend.common.I18nMessageException;
import com.renote.backend.dto.LoginRequest;
import com.renote.backend.dto.RegisterRequest;
import com.renote.backend.entity.User;
import com.renote.backend.enums.UserStatus;
import com.renote.backend.mapper.UserMapper;
import com.renote.backend.security.JwtTokenProvider;
import com.renote.backend.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    @Test
    void loginFailsWhenUsernameMissing() {
        UserMapper userMapper = mock(UserMapper.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        JwtTokenProvider jwt = mock(JwtTokenProvider.class);
        AuthServiceImpl service = new AuthServiceImpl(userMapper, encoder, jwt);

        when(userMapper.findByUsername("nouser")).thenReturn(null);

        LoginRequest req = new LoginRequest();
        req.setUsername("nouser");
        req.setPassword("any");

        I18nMessageException ex = assertThrows(I18nMessageException.class, () -> service.login(req));
        assertEquals("error.auth.usernameNotFound", ex.getMessageKey());
    }

    @Test
    void loginFailsWhenPasswordWrong() {
        UserMapper userMapper = mock(UserMapper.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        JwtTokenProvider jwt = mock(JwtTokenProvider.class);
        AuthServiceImpl service = new AuthServiceImpl(userMapper, encoder, jwt);

        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setPasswordHash("hash");
        user.setStatus(UserStatus.ACTIVE.code());
        when(userMapper.findByUsername("alice")).thenReturn(user);
        when(encoder.matches("wrong", "hash")).thenReturn(false);

        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("wrong");

        I18nMessageException ex = assertThrows(I18nMessageException.class, () -> service.login(req));
        assertEquals("error.auth.passwordIncorrect", ex.getMessageKey());
    }

    @Test
    void registerFailsWhenUsernameTaken() {
        UserMapper userMapper = mock(UserMapper.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        JwtTokenProvider jwt = mock(JwtTokenProvider.class);
        AuthServiceImpl service = new AuthServiceImpl(userMapper, encoder, jwt);

        User existing = new User();
        existing.setId(9L);
        when(userMapper.findByUsername("taken")).thenReturn(existing);

        RegisterRequest req = new RegisterRequest();
        req.setUsername("taken");
        req.setPassword("secret12");

        I18nMessageException ex = assertThrows(I18nMessageException.class, () -> service.register(req));
        assertEquals("error.auth.usernameDuplicate", ex.getMessageKey());
    }
}
