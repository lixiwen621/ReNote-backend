package com.renote.backend.service;

import com.renote.backend.dto.CreateReviewTaskRequest;
import com.renote.backend.dto.ReviewTaskResponse;

public interface ReviewTaskService {

    ReviewTaskResponse createTask(CreateReviewTaskRequest request);

    ReviewTaskResponse getTask(Long taskId);
}
