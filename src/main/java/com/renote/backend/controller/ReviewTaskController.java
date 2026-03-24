package com.renote.backend.controller;

import com.renote.backend.common.ApiResponse;
import com.renote.backend.dto.CreateReviewTaskRequest;
import com.renote.backend.dto.ReviewTaskResponse;
import com.renote.backend.service.ReviewTaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/review-tasks")
public class ReviewTaskController {

    private final ReviewTaskService reviewTaskService;

    public ReviewTaskController(ReviewTaskService reviewTaskService) {
        this.reviewTaskService = reviewTaskService;
    }

    @PostMapping
    public ApiResponse<ReviewTaskResponse> createTask(@Valid @RequestBody CreateReviewTaskRequest request) {
        return ApiResponse.success(reviewTaskService.createTask(request));
    }

    @GetMapping("/{taskId}")
    public ApiResponse<ReviewTaskResponse> getTask(@PathVariable Long taskId) {
        return ApiResponse.success(reviewTaskService.getTask(taskId));
    }
}
