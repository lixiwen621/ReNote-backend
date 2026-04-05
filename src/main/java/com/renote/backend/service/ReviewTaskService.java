package com.renote.backend.service;

import com.renote.backend.dto.CreateReviewTaskRequest;
import com.renote.backend.dto.EditReviewTaskRequest;
import com.renote.backend.dto.EditReviewTaskResponse;
import com.renote.backend.dto.ReminderScheduleResponse;
import com.renote.backend.dto.ReviewCompleteRequest;
import com.renote.backend.dto.ReviewTaskResponse;
import com.renote.backend.dto.UpdateScheduleTimeRequest;
import com.renote.backend.dto.UpdateScheduleTimeResponse;
import com.renote.backend.dto.UpdateTaskNoteContentRequest;
import com.renote.backend.dto.UpdateTaskNoteUrlRequest;

import java.util.List;

public interface ReviewTaskService {

    ReviewTaskResponse createTask(Long userId, CreateReviewTaskRequest request);

    ReviewTaskResponse getTask(Long userId, Long taskId);

    List<ReminderScheduleResponse> getTaskSchedules(Long userId, Long taskId);

    void completeReview(Long userId, Long taskId, ReviewCompleteRequest request);

    UpdateScheduleTimeResponse updateScheduleTime(Long userId, Long taskId, Long scheduleId, UpdateScheduleTimeRequest request);

    /** 更新任务笔记链接（任务详情页编辑） */
    ReviewTaskResponse updateTaskNoteUrl(Long userId, Long taskId, UpdateTaskNoteUrlRequest request);

    /** 更新任务复习内容（富文本 HTML） */
    ReviewTaskResponse updateTaskNoteContent(Long userId, Long taskId, UpdateTaskNoteContentRequest request);

    /** 统一编辑：提醒时间 / 任务链接 / 复习内容（任意字段可选） */
    EditReviewTaskResponse editReviewTask(Long userId, Long taskId, EditReviewTaskRequest request);
}
