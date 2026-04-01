package com.renote.backend.service;

import com.renote.backend.dto.CreateReviewTaskRequest;
import com.renote.backend.dto.ReminderScheduleResponse;
import com.renote.backend.dto.ReviewCompleteRequest;
import com.renote.backend.dto.ReviewTaskResponse;
import com.renote.backend.dto.UpdateScheduleTimeRequest;
import com.renote.backend.dto.UpdateScheduleTimeResponse;

import java.util.List;

public interface ReviewTaskService {

    ReviewTaskResponse createTask(Long userId, CreateReviewTaskRequest request);

    ReviewTaskResponse getTask(Long userId, Long taskId);

    List<ReminderScheduleResponse> getTaskSchedules(Long userId, Long taskId);

    void completeReview(Long userId, Long taskId, ReviewCompleteRequest request);

    UpdateScheduleTimeResponse updateScheduleTime(Long userId, Long taskId, Long scheduleId, UpdateScheduleTimeRequest request);
}
