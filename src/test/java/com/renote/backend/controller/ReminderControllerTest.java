package com.renote.backend.controller;

import com.renote.backend.config.SecurityConfig;
import com.renote.backend.service.ReminderDispatchService;
import com.renote.backend.service.impl.ReminderDispatchServiceImpl;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import com.renote.backend.security.JwtAuthenticationFilter;
import com.renote.backend.security.JwtTokenProvider;
import com.renote.backend.security.RestAuthenticationEntryPoint;
import org.mybatis.spring.SqlSessionTemplate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ReminderController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = ReminderDispatchServiceImpl.class))
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class})
class ReminderControllerTest {

    private static final Long USER_ID = 1L;
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_TOKEN = "Bearer test-jwt";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReminderDispatchService reminderDispatchService;

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
    void shouldDispatchSuccessfully() throws Exception {
        Mockito.doNothing().when(reminderDispatchService).dispatchDueReminders();

        mockMvc.perform(post("/api/reminders/dispatch")
                        .header(AUTH_HEADER, BEARER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
