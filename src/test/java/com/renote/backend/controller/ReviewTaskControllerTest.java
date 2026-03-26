package com.renote.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renote.backend.dto.CreateReviewTaskRequest;
import com.renote.backend.dto.ReviewCompleteRequest;
import com.renote.backend.dto.ReviewTaskOverviewResponse;
import com.renote.backend.dto.ReviewTaskResponse;
import com.renote.backend.dto.TodayReviewTaskCardResponse;
import com.renote.backend.service.ReviewTaskService;
import com.renote.backend.service.ReviewOverviewService;
import com.renote.backend.security.JwtTokenProvider;
import org.mybatis.spring.SqlSessionTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReviewTaskController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class ReviewTaskControllerTest {

    private static final Long USER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewTaskService reviewTaskService;

    @MockBean
    private ReviewOverviewService reviewOverviewService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private SqlSessionTemplate sqlSessionTemplate;

    @BeforeEach
    void setAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID, null, Collections.emptyList()));
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateTaskSuccessfully() throws Exception {
        CreateReviewTaskRequest request = new CreateReviewTaskRequest();
        request.setTitle("Java并发笔记");
        request.setSourceType(1);
        request.setNoteUrl("https://example.com/note/1");

        ReviewTaskResponse response = ReviewTaskResponse.builder()
                .id(1001L)
                .userId(1L)
                .title("Java并发笔记")
                .sourceType(1)
                .noteUrl("https://example.com/note/1")
                .timezone("Asia/Shanghai")
                .scheduleMode(2)
                .status(1)
                .build();

        Mockito.when(reviewTaskService.createTask(eq(USER_ID), any(CreateReviewTaskRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/review-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.id").value(1001))
                .andExpect(jsonPath("$.data.title").value("Java并发笔记"));
    }

    @Test
    void shouldReturnValidationErrorWhenTitleMissing() throws Exception {
        CreateReviewTaskRequest request = new CreateReviewTaskRequest();
        request.setSourceType(1);

        mockMvc.perform(post("/api/review-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldGetTaskSuccessfully() throws Exception {
        ReviewTaskResponse response = ReviewTaskResponse.builder()
                .id(1001L)
                .userId(1L)
                .title("Java并发笔记")
                .sourceType(1)
                .noteUrl("https://example.com/note/1")
                .timezone("Asia/Shanghai")
                .scheduleMode(2)
                .status(1)
                .build();

        Mockito.when(reviewTaskService.getTask(USER_ID, 1001L)).thenReturn(response);

        mockMvc.perform(get("/api/review-tasks/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1001))
                .andExpect(jsonPath("$.data.status").value(1));
    }

    @Test
    void shouldCompleteReviewSuccessfully() throws Exception {
        ReviewCompleteRequest request = new ReviewCompleteRequest();
        request.setReviewResult(1);
        request.setConfidenceScore(4);
        request.setNote("已完成一次复习");

        mockMvc.perform(post("/api/review-tasks/1001/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void shouldGetTodayOverviewSuccessfully() throws Exception {
        ReviewTaskOverviewResponse response = new ReviewTaskOverviewResponse();
        response.setDueTaskCount(1);
        response.setDueReminderCount(2);
        response.setCompletedTodayCount(0);

        ReviewTaskOverviewResponse.NextUpTaskResponse nextUp = new ReviewTaskOverviewResponse.NextUpTaskResponse();
        nextUp.setTaskId(1001L);
        nextUp.setTitle("Java并发笔记");
        nextUp.setNextRemindAt(java.time.LocalDateTime.of(2026, 3, 25, 9, 0, 0));
        response.setNextUp(nextUp);

        Mockito.when(reviewOverviewService.getTodayOverview(eq(USER_ID), any(java.time.LocalDate.class)))
                .thenReturn(response);

        mockMvc.perform(get("/api/review-tasks/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.dueTaskCount").value(1))
                .andExpect(jsonPath("$.data.dueReminderCount").value(2))
                .andExpect(jsonPath("$.data.nextUp.taskId").value(1001))
                .andExpect(jsonPath("$.data.nextUp.title").value("Java并发笔记"));
    }

    @Test
    void shouldGetTodayTaskCardsSuccessfully() throws Exception {
        TodayReviewTaskCardResponse card = new TodayReviewTaskCardResponse();
        card.setTaskId(1001L);
        card.setTitle("Java并发笔记");
        card.setScheduledAt(java.time.LocalDateTime.of(2026, 3, 25, 9, 0, 0));
        card.setScheduleId(2001L);
        card.setScheduleStatus(1);
        card.setCanComplete(true);

        Mockito.when(reviewOverviewService.getTodayTaskCards(eq(USER_ID), any(java.time.LocalDate.class)))
                .thenReturn(List.of(card));

        mockMvc.perform(get("/api/review-tasks/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].taskId").value(1001))
                .andExpect(jsonPath("$.data[0].title").value("Java并发笔记"))
                .andExpect(jsonPath("$.data[0].scheduleId").value(2001))
                .andExpect(jsonPath("$.data[0].scheduleStatus").value(1))
                .andExpect(jsonPath("$.data[0].canComplete").value(true));
    }
}
