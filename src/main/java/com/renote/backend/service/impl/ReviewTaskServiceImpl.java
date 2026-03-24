package com.renote.backend.service.impl;

import com.renote.backend.dto.CreateReviewTaskRequest;
import com.renote.backend.dto.ReviewTaskResponse;
import com.renote.backend.entity.ReviewTask;
import com.renote.backend.mapper.ReviewTaskMapper;
import com.renote.backend.service.ReviewTaskService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ReviewTaskServiceImpl implements ReviewTaskService {

    private final ReviewTaskMapper reviewTaskMapper;

    public ReviewTaskServiceImpl(ReviewTaskMapper reviewTaskMapper) {
        this.reviewTaskMapper = reviewTaskMapper;
    }

    @Override
    public ReviewTaskResponse createTask(CreateReviewTaskRequest request) {
        ReviewTask task = new ReviewTask();
        task.setUserId(request.getUserId() == null ? 1L : request.getUserId());
        task.setTitle(request.getTitle());
        task.setSourceType(request.getSourceType());
        task.setNoteUrl(request.getNoteUrl());
        task.setNoteContent(request.getNoteContent());
        task.setTimezone(StringUtils.hasText(request.getTimezone()) ? request.getTimezone() : "Asia/Shanghai");
        task.setScheduleMode(StringUtils.hasText(request.getScheduleMode()) ? request.getScheduleMode() : "forgetting_curve");
        task.setStatus("active");

        reviewTaskMapper.insert(task);
        return toResponse(task);
    }

    @Override
    public ReviewTaskResponse getTask(Long taskId) {
        ReviewTask task = reviewTaskMapper.findById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        return toResponse(task);
    }

    private ReviewTaskResponse toResponse(ReviewTask task) {
        return ReviewTaskResponse.builder()
                .id(task.getId())
                .userId(task.getUserId())
                .title(task.getTitle())
                .sourceType(task.getSourceType())
                .noteUrl(task.getNoteUrl())
                .noteContent(task.getNoteContent())
                .timezone(task.getTimezone())
                .scheduleMode(task.getScheduleMode())
                .status(task.getStatus())
                .build();
    }
}
