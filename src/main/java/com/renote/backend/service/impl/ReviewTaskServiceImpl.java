package com.renote.backend.service.impl;

import com.renote.backend.dto.CreateReviewTaskRequest;
import com.renote.backend.dto.ReminderScheduleResponse;
import com.renote.backend.dto.ReviewCompleteRequest;
import com.renote.backend.dto.ReviewTaskResponse;
import com.renote.backend.entity.ReminderSchedule;
import com.renote.backend.entity.ReviewTask;
import com.renote.backend.entity.ReviewRecord;
import com.renote.backend.enums.NoteSourceType;
import com.renote.backend.enums.ReminderScheduleStatus;
import com.renote.backend.enums.ReviewResult;
import com.renote.backend.enums.ReviewTaskStatus;
import com.renote.backend.enums.ScheduleMode;
import com.renote.backend.mapper.ReminderScheduleMapper;
import com.renote.backend.mapper.ReviewTaskMapper;
import com.renote.backend.mapper.ReviewRecordMapper;
import com.renote.backend.service.ReviewTaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ReviewTaskServiceImpl implements ReviewTaskService {

    private final ReviewTaskMapper reviewTaskMapper;
    private final ReminderScheduleMapper reminderScheduleMapper;
    private final ReviewRecordMapper reviewRecordMapper;

    public ReviewTaskServiceImpl(ReviewTaskMapper reviewTaskMapper,
                                 ReminderScheduleMapper reminderScheduleMapper,
                                 ReviewRecordMapper reviewRecordMapper) {
        this.reviewTaskMapper = reviewTaskMapper;
        this.reminderScheduleMapper = reminderScheduleMapper;
        this.reviewRecordMapper = reviewRecordMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewTaskResponse createTask(CreateReviewTaskRequest request) {
        Integer scheduleModeCode = request.getScheduleMode() == null
                ? ScheduleMode.FORGETTING_CURVE.code()
                : ScheduleMode.fromCode(request.getScheduleMode()).code();
        ReviewTask task = new ReviewTask();
        task.setUserId(request.getUserId() == null ? 1L : request.getUserId());
        task.setTitle(request.getTitle());
        task.setSourceType(NoteSourceType.fromCode(request.getSourceType()).code());
        task.setNoteUrl(request.getNoteUrl());
        task.setNoteContent(request.getNoteContent());
        task.setTimezone(StringUtils.hasText(request.getTimezone()) ? request.getTimezone() : "Asia/Shanghai");
        task.setScheduleMode(scheduleModeCode);
        task.setStatus(ReviewTaskStatus.ACTIVE.code());
        reviewTaskMapper.insert(task);

        List<LocalDateTime> remindTimes = buildRemindTimes(request, scheduleModeCode);
        LocalDateTime nextRemindAt = null;
        for (LocalDateTime remindTime : remindTimes) {
            ReminderSchedule schedule = new ReminderSchedule();
            schedule.setTaskId(task.getId());
            schedule.setUserId(task.getUserId());
            schedule.setScheduledAt(remindTime);
            schedule.setStatus(ReminderScheduleStatus.PENDING.code());
            schedule.setAttemptCount(0);
            schedule.setMaxAttempts(3);
            schedule.setIdempotencyKey(task.getId() + "-" + remindTime + "-" + UUID.randomUUID());
            reminderScheduleMapper.insert(schedule);
            if (nextRemindAt == null || remindTime.isBefore(nextRemindAt)) {
                nextRemindAt = remindTime;
            }
        }
        if (nextRemindAt != null) {
            reviewTaskMapper.updateNextRemindAt(task.getId(), nextRemindAt);
        }
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

    @Override
    public List<ReminderScheduleResponse> getTaskSchedules(Long taskId) {
        ensureTaskExists(taskId);
        List<ReminderSchedule> schedules = reminderScheduleMapper.findByTaskId(taskId);
        List<ReminderScheduleResponse> responses = new ArrayList<>();
        for (ReminderSchedule schedule : schedules) {
            responses.add(ReminderScheduleResponse.builder()
                    .id(schedule.getId())
                    .taskId(schedule.getTaskId())
                    .userId(schedule.getUserId())
                    .scheduledAt(schedule.getScheduledAt())
                    .status(schedule.getStatus())
                    .attemptCount(schedule.getAttemptCount())
                    .sentAt(schedule.getSentAt())
                    .failReason(schedule.getFailReason())
                    .build());
        }
        return responses;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeReview(Long taskId, ReviewCompleteRequest request) {
        ReviewTask task = ensureTaskExists(taskId);
        ReviewRecord record = new ReviewRecord();
        record.setTaskId(taskId);
        record.setUserId(request.getUserId() == null ? task.getUserId() : request.getUserId());
        record.setScheduleId(request.getScheduleId());
        record.setReviewedAt(LocalDateTime.now());
        record.setReviewResult(ReviewResult.fromCode(request.getReviewResult()).code());
        record.setConfidenceScore(request.getConfidenceScore());
        record.setNote(request.getNote());
        reviewRecordMapper.insert(record);
        reviewTaskMapper.updateLastReviewedAt(taskId, record.getReviewedAt());

        List<ReminderSchedule> remains = reminderScheduleMapper.findByTaskId(taskId);
        LocalDateTime next = remains.stream()
                .filter(s -> Integer.valueOf(ReminderScheduleStatus.PENDING.code()).equals(s.getStatus()))
                .map(ReminderSchedule::getScheduledAt)
                .min(Comparator.naturalOrder())
                .orElse(null);
        reviewTaskMapper.updateNextRemindAt(taskId, next);
    }

    private ReviewTask ensureTaskExists(Long taskId) {
        ReviewTask task = reviewTaskMapper.findById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        return task;
    }

    private List<LocalDateTime> buildRemindTimes(CreateReviewTaskRequest request, Integer scheduleMode) {
        if (Integer.valueOf(ScheduleMode.MANUAL.code()).equals(scheduleMode)
                && request.getRemindTimes() != null
                && !request.getRemindTimes().isEmpty()) {
            List<LocalDateTime> list = new ArrayList<>(request.getRemindTimes());
            list.sort(LocalDateTime::compareTo);
            return list;
        }

        LocalDateTime now = LocalDateTime.now();
        return List.of(
                now.plusDays(1),
                now.plusDays(2),
                now.plusDays(4),
                now.plusDays(7),
                now.plusDays(15)
        );
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
