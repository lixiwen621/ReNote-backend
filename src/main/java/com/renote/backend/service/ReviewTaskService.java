package com.renote.backend.service;

import com.renote.backend.dto.CreateReviewTaskRequest;
import com.renote.backend.dto.ReminderScheduleResponse;
import com.renote.backend.dto.ReviewCompleteRequest;
import com.renote.backend.dto.ReviewTaskResponse;

import java.util.List;

public interface ReviewTaskService {

    ReviewTaskResponse createTask(CreateReviewTaskRequest request);

    ReviewTaskResponse getTask(Long taskId);

    List<ReminderScheduleResponse> getTaskSchedules(Long taskId);

    void completeReview(Long taskId, ReviewCompleteRequest request);
}
