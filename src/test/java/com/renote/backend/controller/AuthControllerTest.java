package com.renote.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renote.backend.dto.CurrentUserResponse;
import com.renote.backend.dto.LoginRequest;
import com.renote.backend.dto.LoginResponse;
import com.renote.backend.dto.RegisterRequest;
import com.renote.backend.config.SecurityConfig;
import com.renote.backend.service.AuthService;
import com.renote.backend.service.impl.AuthServiceImpl;
import com.renote.backend.security.JwtAuthenticationFilter;
import com.renote.backend.security.JwtTokenProvider;
import com.renote.backend.security.RestAuthenticationEntryPoint;
import org.mybatis.spring.SqlSessionTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = AuthServiceImpl.class))
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class})
class AuthControllerTest {

    private static final Long USER_ID = 1L;
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_TOKEN = "Bearer test-jwt";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private SqlSessionTemplate sqlSessionTemplate;

    @BeforeEach
    void stubJwt() {
        Mockito.when(jwtTokenProvider.parseUserId(org.mockito.ArgumentMatchers.anyString())).thenReturn(USER_ID);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRegisterSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setPassword("secret12");

        LoginResponse response = LoginResponse.builder()
                .accessToken("mock-token")
                .tokenType("Bearer")
                .expiresInMs(86400000L)
                .userId(USER_ID)
                .build();
        Mockito.when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(1));
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("secret12");

        LoginResponse response = LoginResponse.builder()
                .accessToken("mock-token")
                .tokenType("Bearer")
                .expiresInMs(86400000L)
                .userId(USER_ID)
                .build();
        Mockito.when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    void shouldGetCurrentUserByTokenSuccessfully() throws Exception {
        CurrentUserResponse response = CurrentUserResponse.builder()
                .userId(USER_ID)
                .username("alice")
                .build();
        Mockito.when(authService.getCurrentUser(eq(USER_ID))).thenReturn(response);

        mockMvc.perform(get("/api/auth/me")
                        .header(AUTH_HEADER, BEARER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.username").value("alice"));
    }
}
