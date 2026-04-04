package com.renote.backend.controller;

import com.renote.backend.common.ApiResponse;
import com.renote.backend.dto.CreateReviewTaskRequest;
import com.renote.backend.dto.ReminderScheduleResponse;
import com.renote.backend.dto.ReviewTaskOverviewResponse;
import com.renote.backend.dto.ReviewCompleteRequest;
import com.renote.backend.dto.ReviewTaskResponse;
import com.renote.backend.dto.TodayReviewTaskCardResponse;
import com.renote.backend.dto.UpdateScheduleTimeRequest;
import com.renote.backend.dto.UpdateScheduleTimeResponse;
import com.renote.backend.dto.UpdateTaskNoteUrlRequest;
import com.renote.backend.dto.WeekReviewScheduleResponse;
import com.renote.backend.service.ReviewTaskService;
import com.renote.backend.service.ReviewOverviewService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/review-tasks")
public class ReviewTaskController {

    private final ReviewTaskService reviewTaskService;
    private final ReviewOverviewService reviewOverviewService;

    public ReviewTaskController(ReviewTaskService reviewTaskService, ReviewOverviewService reviewOverviewService) {
        this.reviewTaskService = reviewTaskService;
        this.reviewOverviewService = reviewOverviewService;
    }

    @PostMapping
    public ApiResponse<ReviewTaskResponse> createTask(
            Authentication authentication,
            @Valid @RequestBody CreateReviewTaskRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(reviewTaskService.createTask(userId, request));
    }

    @GetMapping("/{taskId}")
    public ApiResponse<ReviewTaskResponse> getTask(Authentication authentication, @PathVariable Long taskId) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(reviewTaskService.getTask(userId, taskId));
    }

    @PatchMapping("/{taskId}/note-url")
    public ApiResponse<ReviewTaskResponse> updateTaskNoteUrl(
            Authentication authentication,
            @PathVariable Long taskId,
            @Valid @RequestBody UpdateTaskNoteUrlRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(reviewTaskService.updateTaskNoteUrl(userId, taskId, request));
    }

    @GetMapping("/{taskId}/schedules")
    public ApiResponse<List<ReminderScheduleResponse>> getTaskSchedules(
            Authentication authentication,
            @PathVariable Long taskId) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(reviewTaskService.getTaskSchedules(userId, taskId));
    }

    @PostMapping("/{taskId}/complete")
    public ApiResponse<Void> completeReview(
            Authentication authentication,
            @PathVariable Long taskId,
            @Valid @RequestBody ReviewCompleteRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        reviewTaskService.completeReview(userId, taskId, request);
        return ApiResponse.success(null);
    }

    @PatchMapping("/{taskId}/schedules/{scheduleId}/time")
    public ApiResponse<UpdateScheduleTimeResponse> updateScheduleTime(
            Authentication authentication,
            @PathVariable Long taskId,
            @PathVariable Long scheduleId,
            @Valid @RequestBody UpdateScheduleTimeRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(reviewTaskService.updateScheduleTime(userId, taskId, scheduleId, request));
    }

    @GetMapping("/overview")
    public ApiResponse<ReviewTaskOverviewResponse> overview(
            Authentication authentication,
            @RequestParam(required = false) String date) {
        Long userId = (Long) authentication.getPrincipal();
        LocalDate localDate = (date == null || date.isBlank()) ? LocalDate.now() : LocalDate.parse(date);
        return ApiResponse.success(reviewOverviewService.getTodayOverview(userId, localDate));
    }

    @GetMapping("/today")
    public ApiResponse<List<TodayReviewTaskCardResponse>> todayTasks(
            Authentication authentication,
            @RequestParam(required = false) String date) {
        Long userId = (Long) authentication.getPrincipal();
        LocalDate localDate = (date == null || date.isBlank()) ? LocalDate.now() : LocalDate.parse(date);
        return ApiResponse.success(reviewOverviewService.getTodayTaskCards(userId, localDate));
    }

    @GetMapping("/week")
    public ApiResponse<WeekReviewScheduleResponse> weekSchedule(
            Authentication authentication,
            @RequestParam(required = false) String date) {
        Long userId = (Long) authentication.getPrincipal();
        LocalDate anchor = (date == null || date.isBlank()) ? LocalDate.now() : LocalDate.parse(date);
        return ApiResponse.success(reviewOverviewService.getWeekSchedule(userId, anchor));
    }
}
