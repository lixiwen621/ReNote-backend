package com.renote.backend.service.impl;

import com.renote.backend.dto.CreateReviewTaskRequest;
import com.renote.backend.dto.ReminderScheduleResponse;
import com.renote.backend.dto.ReviewCompleteRequest;
import com.renote.backend.dto.ReviewTaskResponse;
import com.renote.backend.dto.UpdateScheduleTimeRequest;
import com.renote.backend.dto.UpdateScheduleTimeResponse;
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

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ReviewTaskServiceImpl implements ReviewTaskService {

    /** 遗忘曲线自动排期：各次提醒在该日期的固定时刻（用户时区 wall-clock，与 {@code timezone} 字段语义一致） */
    private static final LocalTime DEFAULT_FORGETTING_CURVE_REMIND_TIME = LocalTime.of(14, 30);

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
    public ReviewTaskResponse createTask(Long userId, CreateReviewTaskRequest request) {
        Integer scheduleModeCode = request.getScheduleMode() == null
                ? ScheduleMode.FORGETTING_CURVE.code()
                : ScheduleMode.fromCode(request.getScheduleMode()).code();
        ReviewTask task = new ReviewTask();
        task.setUserId(userId);
        task.setTitle(request.getTitle());
        task.setSourceType(NoteSourceType.fromCode(request.getSourceType()).code());
        task.setNoteUrl(request.getNoteUrl());
        task.setNoteContent(request.getNoteContent());
        task.setTimezone(StringUtils.hasText(request.getTimezone()) ? request.getTimezone() : "Asia/Shanghai");
        task.setScheduleMode(scheduleModeCode);
        task.setStatus(ReviewTaskStatus.ACTIVE.code());
        reviewTaskMapper.insert(task);

        List<LocalDateTime> remindTimes = buildRemindTimes(request, scheduleModeCode, task.getTimezone());
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
    public ReviewTaskResponse getTask(Long userId, Long taskId) {
        ReviewTask task = reviewTaskMapper.findByIdAndUserId(taskId, userId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        return toResponse(task);
    }

    @Override
    public List<ReminderScheduleResponse> getTaskSchedules(Long userId, Long taskId) {
        ensureTaskExistsForUser(taskId, userId);
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
    public void completeReview(Long userId, Long taskId, ReviewCompleteRequest request) {
        ensureTaskExistsForUser(taskId, userId);

        Long scheduleId = request.getScheduleId();
        if (scheduleId != null) {
            ReminderSchedule schedule = reminderScheduleMapper.findByIdAndUserId(scheduleId, userId);
            if (schedule == null || !taskId.equals(schedule.getTaskId())) {
                throw new IllegalArgumentException("提醒计划不存在或不属于该任务: " + scheduleId);
            }
            if (reviewRecordMapper.countByUserIdAndScheduleId(userId, scheduleId) > 0) {
                reminderScheduleMapper.markSentIfPending(scheduleId);
                return;
            }
        }

        ReviewRecord record = new ReviewRecord();
        record.setTaskId(taskId);
        record.setUserId(userId);
        record.setScheduleId(scheduleId);
        record.setReviewedAt(LocalDateTime.now());
        record.setReviewResult(ReviewResult.fromCode(request.getReviewResult()).code());
        record.setConfidenceScore(request.getConfidenceScore());
        record.setNote(request.getNote());
        reviewRecordMapper.insert(record);
        reviewTaskMapper.updateLastReviewedAt(taskId, record.getReviewedAt());

        if (scheduleId != null) {
            reminderScheduleMapper.markSentIfPending(scheduleId);
        }

        List<ReminderSchedule> remains = reminderScheduleMapper.findByTaskId(taskId);
        LocalDateTime next = remains.stream()
                .filter(s -> Integer.valueOf(ReminderScheduleStatus.PENDING.code()).equals(s.getStatus()))
                .map(ReminderSchedule::getScheduledAt)
                .min(Comparator.naturalOrder())
                .orElse(null);
        reviewTaskMapper.updateNextRemindAt(taskId, next);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UpdateScheduleTimeResponse updateScheduleTime(Long userId,
                                                         Long taskId,
                                                         Long scheduleId,
                                                         UpdateScheduleTimeRequest request) {
        ensureTaskExistsForUser(taskId, userId);

        ReminderSchedule schedule = reminderScheduleMapper.findByIdAndUserId(scheduleId, userId);
        if (schedule == null || !taskId.equals(schedule.getTaskId())) {
            throw new IllegalArgumentException("提醒计划不存在或不属于该任务");
        }

        if (Integer.valueOf(ReminderScheduleStatus.CANCELLED.code()).equals(schedule.getStatus())) {
            throw new IllegalArgumentException("该排期已取消，不可修改");
        }
        if (Integer.valueOf(ReminderScheduleStatus.SENDING.code()).equals(schedule.getStatus())) {
            throw new IllegalArgumentException("发送中排期暂不支持修改，请稍后重试");
        }
        if (reviewRecordMapper.countByUserIdAndScheduleId(userId, scheduleId) > 0) {
            throw new IllegalArgumentException("该排期已完成复习，不可修改");
        }

        LocalDateTime newScheduledAt = request.getScheduledAt();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentMinute = now.truncatedTo(ChronoUnit.MINUTES);
        if (newScheduledAt.truncatedTo(ChronoUnit.MINUTES).equals(currentMinute)) {
            throw new IllegalArgumentException("提醒时间必须晚于当前分钟");
        }
        if (newScheduledAt.isBefore(now)) {
            throw new IllegalArgumentException("提醒时间不能早于当前时间");
        }

        reminderScheduleMapper.updateScheduledAtByIdAndUserId(scheduleId, userId, newScheduledAt);

        List<ReminderSchedule> remains = reminderScheduleMapper.findByTaskId(taskId);
        LocalDateTime next = remains.stream()
                .filter(s -> Integer.valueOf(ReminderScheduleStatus.PENDING.code()).equals(s.getStatus()))
                .map(ReminderSchedule::getScheduledAt)
                .min(Comparator.naturalOrder())
                .orElse(null);
        reviewTaskMapper.updateNextRemindAt(taskId, next);

        ReminderSchedule updated = reminderScheduleMapper.findByIdAndUserId(scheduleId, userId);
        return UpdateScheduleTimeResponse.builder()
                .scheduleId(updated.getId())
                .taskId(updated.getTaskId())
                .scheduledAt(updated.getScheduledAt())
                .scheduleStatus(updated.getStatus())
                .build();
    }

    private ReviewTask ensureTaskExistsForUser(Long taskId, Long userId) {
        ReviewTask task = reviewTaskMapper.findByIdAndUserId(taskId, userId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        return task;
    }

    private List<LocalDateTime> buildRemindTimes(CreateReviewTaskRequest request, Integer scheduleMode, String timezone) {
        if (Integer.valueOf(ScheduleMode.MANUAL.code()).equals(scheduleMode)
                && request.getRemindTimes() != null
                && !request.getRemindTimes().isEmpty()) {
            List<LocalDateTime> list = new ArrayList<>(request.getRemindTimes());
            list.sort(LocalDateTime::compareTo);
            return list;
        }

        LocalDate todayInUserZone = LocalDate.now(resolveZoneId(timezone));
        return List.of(
                LocalDateTime.of(todayInUserZone.plusDays(1), DEFAULT_FORGETTING_CURVE_REMIND_TIME),
                LocalDateTime.of(todayInUserZone.plusDays(2), DEFAULT_FORGETTING_CURVE_REMIND_TIME),
                LocalDateTime.of(todayInUserZone.plusDays(4), DEFAULT_FORGETTING_CURVE_REMIND_TIME),
                LocalDateTime.of(todayInUserZone.plusDays(7), DEFAULT_FORGETTING_CURVE_REMIND_TIME),
                LocalDateTime.of(todayInUserZone.plusDays(15), DEFAULT_FORGETTING_CURVE_REMIND_TIME)
        );
    }

    private static ZoneId resolveZoneId(String timezone) {
        if (!StringUtils.hasText(timezone)) {
            return ZoneId.of("Asia/Shanghai");
        }
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException ex) {
            return ZoneId.of("Asia/Shanghai");
        }
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
