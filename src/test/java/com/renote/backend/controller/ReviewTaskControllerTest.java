package com.renote.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renote.backend.dto.CreateReviewTaskRequest;
import com.renote.backend.dto.ReviewCompleteRequest;
import com.renote.backend.dto.ReviewTaskOverviewResponse;
import com.renote.backend.dto.ReviewTaskResponse;
import com.renote.backend.dto.TodayReviewTaskCardResponse;
import com.renote.backend.dto.UpdateScheduleTimeRequest;
import com.renote.backend.dto.UpdateScheduleTimeResponse;
import com.renote.backend.dto.WeekReviewDayResponse;
import com.renote.backend.dto.WeekReviewScheduleResponse;
import com.renote.backend.dto.WeekReviewTaskCardResponse;
import com.renote.backend.config.SecurityConfig;
import com.renote.backend.service.ReviewTaskService;
import com.renote.backend.service.ReviewOverviewService;
import com.renote.backend.service.impl.ReviewOverviewServiceImpl;
import com.renote.backend.service.impl.ReviewTaskServiceImpl;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ReviewTaskController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {ReviewTaskServiceImpl.class, ReviewOverviewServiceImpl.class}))
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, RestAuthenticationEntryPoint.class})
class ReviewTaskControllerTest {

    private static final Long USER_ID = 1L;
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_TOKEN = "Bearer test-jwt";

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
    void stubJwt() {
        Mockito.when(jwtTokenProvider.parseUserId(org.mockito.ArgumentMatchers.anyString())).thenReturn(USER_ID);
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
                        .header(AUTH_HEADER, BEARER_TOKEN)
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
                        .header(AUTH_HEADER, BEARER_TOKEN)
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

        mockMvc.perform(get("/api/review-tasks/1001")
                        .header(AUTH_HEADER, BEARER_TOKEN))
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
                        .header(AUTH_HEADER, BEARER_TOKEN)
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
        response.setPendingNotifyReminderCount(1);
        response.setCompletedTodayCount(0);

        ReviewTaskOverviewResponse.NextUpTaskResponse nextUp = new ReviewTaskOverviewResponse.NextUpTaskResponse();
        nextUp.setTaskId(1001L);
        nextUp.setTitle("Java并发笔记");
        nextUp.setNextRemindAt(java.time.LocalDateTime.of(2026, 3, 25, 9, 0, 0));
        response.setNextUp(nextUp);

        Mockito.when(reviewOverviewService.getTodayOverview(eq(USER_ID), any(java.time.LocalDate.class)))
                .thenReturn(response);

        mockMvc.perform(get("/api/review-tasks/overview")
                        .header(AUTH_HEADER, BEARER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.dueTaskCount").value(1))
                .andExpect(jsonPath("$.data.dueReminderCount").value(2))
                .andExpect(jsonPath("$.data.pendingNotifyReminderCount").value(1))
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
        card.setReminderNotifyPhase(1);
        card.setCanComplete(true);

        Mockito.when(reviewOverviewService.getTodayTaskCards(eq(USER_ID), any(java.time.LocalDate.class)))
                .thenReturn(List.of(card));

        mockMvc.perform(get("/api/review-tasks/today")
                        .header(AUTH_HEADER, BEARER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].taskId").value(1001))
                .andExpect(jsonPath("$.data[0].title").value("Java并发笔记"))
                .andExpect(jsonPath("$.data[0].scheduleId").value(2001))
                .andExpect(jsonPath("$.data[0].scheduleStatus").value(1))
                .andExpect(jsonPath("$.data[0].reminderNotifyPhase").value(1))
                .andExpect(jsonPath("$.data[0].canComplete").value(true));
    }

    @Test
    void shouldGetWeekScheduleSuccessfully() throws Exception {
        WeekReviewScheduleResponse week = new WeekReviewScheduleResponse();
        week.setWeekStart(java.time.LocalDate.of(2026, 3, 16));
        week.setWeekEnd(java.time.LocalDate.of(2026, 3, 22));
        WeekReviewDayResponse mon = new WeekReviewDayResponse();
        mon.setDate(java.time.LocalDate.of(2026, 3, 16));
        WeekReviewTaskCardResponse card = new WeekReviewTaskCardResponse();
        card.setTaskId(1001L);
        card.setTitle("Java并发笔记");
        card.setScheduledAt(java.time.LocalDateTime.of(2026, 3, 16, 9, 0, 0));
        card.setScheduleId(2001L);
        card.setScheduleStatus(3);
        card.setReminderNotifyPhase(3);
        card.setReviewCompleted(true);
        card.setCanComplete(false);
        mon.setItems(List.of(card));
        week.setDays(List.of(mon));

        Mockito.when(reviewOverviewService.getWeekSchedule(eq(USER_ID), any(java.time.LocalDate.class)))
                .thenReturn(week);

        mockMvc.perform(get("/api/review-tasks/week")
                        .param("date", "2026-03-18")
                        .header(AUTH_HEADER, BEARER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.weekStart").value("2026-03-16"))
                .andExpect(jsonPath("$.data.weekEnd").value("2026-03-22"))
                .andExpect(jsonPath("$.data.days[0].date").value("2026-03-16"))
                .andExpect(jsonPath("$.data.days[0].items[0].reviewCompleted").value(true))
                .andExpect(jsonPath("$.data.days[0].items[0].canComplete").value(false));
    }

    @Test
    void shouldUpdateScheduleTimeSuccessfully() throws Exception {
        UpdateScheduleTimeRequest request = new UpdateScheduleTimeRequest();
        request.setScheduledAt(java.time.LocalDateTime.of(2026, 4, 1, 21, 0, 0));

        UpdateScheduleTimeResponse response = UpdateScheduleTimeResponse.builder()
                .scheduleId(2001L)
                .taskId(1001L)
                .scheduledAt(java.time.LocalDateTime.of(2026, 4, 1, 21, 0, 0))
                .scheduleStatus(1)
                .build();

        Mockito.when(reviewTaskService.updateScheduleTime(eq(USER_ID), eq(1001L), eq(2001L), any(UpdateScheduleTimeRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/review-tasks/1001/schedules/2001/time")
                        .header(AUTH_HEADER, BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.scheduleId").value(2001))
                .andExpect(jsonPath("$.data.taskId").value(1001))
                .andExpect(jsonPath("$.data.scheduledAt").value("2026-04-01T21:00:00"))
                .andExpect(jsonPath("$.data.scheduleStatus").value(1));
    }
}
