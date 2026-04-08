package com.renote.backend.service.impl;

import com.renote.backend.dto.CreateReviewTaskRequest;
import com.renote.backend.dto.EditReviewTaskRequest;
import com.renote.backend.dto.EditReviewTaskResponse;
import com.renote.backend.dto.ReminderScheduleResponse;
import com.renote.backend.dto.ReviewCompleteRequest;
import com.renote.backend.dto.TaskAttachmentResponse;
import com.renote.backend.dto.ReviewTaskResponse;
import com.renote.backend.dto.UpdateScheduleTimeRequest;
import com.renote.backend.dto.UpdateScheduleTimeResponse;
import com.renote.backend.dto.UpdateTaskNoteContentRequest;
import com.renote.backend.dto.UpdateTaskNoteUrlRequest;
import com.renote.backend.entity.ReminderSchedule;
import com.renote.backend.entity.ReviewTask;
import com.renote.backend.entity.ReviewTaskAttachment;
import com.renote.backend.entity.ReviewRecord;
import com.renote.backend.enums.NoteSourceType;
import com.renote.backend.enums.ReminderScheduleStatus;
import com.renote.backend.enums.ReminderStrategy;
import com.renote.backend.enums.ReviewResult;
import com.renote.backend.enums.ReviewTaskStatus;
import com.renote.backend.enums.ScheduleMode;
import com.renote.backend.mapper.ReminderScheduleMapper;
import com.renote.backend.mapper.ReviewTaskMapper;
import com.renote.backend.mapper.ReviewTaskAttachmentMapper;
import com.renote.backend.mapper.ReviewRecordMapper;
import com.renote.backend.common.I18nPreconditions;
import com.renote.backend.config.ForgettingCurveProperties;
import com.renote.backend.service.ReviewTaskService;
import com.renote.backend.config.TaskAttachmentStorageProperties;
import com.renote.backend.service.CosAttachmentUrlService;
import com.renote.backend.service.TaskAttachmentStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

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

    private final ReviewTaskMapper reviewTaskMapper;
    private final ReminderScheduleMapper reminderScheduleMapper;
    private final ReviewRecordMapper reviewRecordMapper;
    private final ReviewTaskAttachmentMapper reviewTaskAttachmentMapper;
    private final ForgettingCurveProperties forgettingCurveProperties;
    private final TaskAttachmentStorageService taskAttachmentStorageService;
    private final TaskAttachmentStorageProperties storageProperties;
    private final CosAttachmentUrlService cosAttachmentUrlService;

    public ReviewTaskServiceImpl(ReviewTaskMapper reviewTaskMapper,
                                 ReminderScheduleMapper reminderScheduleMapper,
                                 ReviewRecordMapper reviewRecordMapper,
                                 ReviewTaskAttachmentMapper reviewTaskAttachmentMapper,
                                 ForgettingCurveProperties forgettingCurveProperties,
                                 TaskAttachmentStorageService taskAttachmentStorageService,
                                 TaskAttachmentStorageProperties storageProperties,
                                 CosAttachmentUrlService cosAttachmentUrlService) {
        this.reviewTaskMapper = reviewTaskMapper;
        this.reminderScheduleMapper = reminderScheduleMapper;
        this.reviewRecordMapper = reviewRecordMapper;
        this.reviewTaskAttachmentMapper = reviewTaskAttachmentMapper;
        this.forgettingCurveProperties = forgettingCurveProperties;
        this.taskAttachmentStorageService = taskAttachmentStorageService;
        this.storageProperties = storageProperties;
        this.cosAttachmentUrlService = cosAttachmentUrlService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewTaskResponse createTask(Long userId, CreateReviewTaskRequest request) {
        return createTaskInternal(userId, request, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewTaskResponse createTaskWithFiles(Long userId, CreateReviewTaskRequest request, List<MultipartFile> files) {
        return createTaskInternal(userId, request, files);
    }

    private ReviewTaskResponse createTaskInternal(Long userId, CreateReviewTaskRequest request, List<MultipartFile> files) {
        Integer scheduleModeInput = request.getScheduleMode() == null
                ? ScheduleMode.FORGETTING_CURVE.code()
                : ScheduleMode.fromCode(request.getScheduleMode()).code();

        int reminderStrategyCode = resolveReminderStrategy(request, scheduleModeInput);
        validateReminderStrategyRequest(request, reminderStrategyCode);

        int scheduleModeCode = reminderStrategyCode == ReminderStrategy.FULL_CUSTOM.code()
                ? ScheduleMode.MANUAL.code()
                : ScheduleMode.FORGETTING_CURVE.code();

        ReviewTask task = new ReviewTask();
        task.setUserId(userId);
        task.setTitle(request.getTitle());
        task.setSourceType(NoteSourceType.fromCode(request.getSourceType()).code());
        task.setNoteUrl(request.getNoteUrl());
        task.setNoteContent(request.getNoteContent());
        task.setTimezone(StringUtils.hasText(request.getTimezone()) ? request.getTimezone() : "Asia/Shanghai");
        task.setScheduleMode(scheduleModeCode);
        task.setReminderStrategy(reminderStrategyCode);
        task.setStatus(ReviewTaskStatus.ACTIVE.code());
        reviewTaskMapper.insert(task);

        List<LocalDateTime> remindTimes = buildRemindTimes(request, reminderStrategyCode, task.getTimezone());
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
        saveAttachments(task.getId(), userId, files);
        return toResponse(task, userId);
    }

    @Override
    public ReviewTaskResponse getTask(Long userId, Long taskId) {
        ReviewTask task = reviewTaskMapper.findByIdAndUserId(taskId, userId);
        I18nPreconditions.checkNotNull(task, "error.task.notFound", taskId);
        return toResponse(task, userId);
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
            I18nPreconditions.checkArgument(schedule != null && taskId.equals(schedule.getTaskId()),
                    "error.schedule.notFoundOrNotBelongsWithId", scheduleId);
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
        I18nPreconditions.checkArgument(schedule != null && taskId.equals(schedule.getTaskId()),
                "error.schedule.notFoundOrNotBelongs");

        I18nPreconditions.checkState(!Integer.valueOf(ReminderScheduleStatus.CANCELLED.code()).equals(schedule.getStatus()),
                "error.schedule.cancelled.cannotReschedule");
        I18nPreconditions.checkState(!Integer.valueOf(ReminderScheduleStatus.SENDING.code()).equals(schedule.getStatus()),
                "error.schedule.sending.cannotReschedule");
        I18nPreconditions.checkState(reviewRecordMapper.countByUserIdAndScheduleId(userId, scheduleId) == 0,
                "error.schedule.reviewCompleted.cannotReschedule");

        validateScheduledAtForNewReminder(request.getScheduledAt());

        reminderScheduleMapper.updateScheduledAtByIdAndUserId(scheduleId, userId, request.getScheduledAt());

        List<ReminderSchedule> remains = reminderScheduleMapper.findByTaskId(taskId);
        LocalDateTime next = remains.stream()
                .filter(s -> Integer.valueOf(ReminderScheduleStatus.PENDING.code()).equals(s.getStatus()))
                .map(ReminderSchedule::getScheduledAt)
                .min(Comparator.naturalOrder())
                .orElse(null);
        reviewTaskMapper.updateNextRemindAt(taskId, next);

        ReminderSchedule updated = reminderScheduleMapper.findByIdAndUserId(scheduleId, userId);
        I18nPreconditions.checkNotNull(updated, "error.schedule.reloadAfterUpdateFailed");
        return UpdateScheduleTimeResponse.builder()
                .scheduleId(updated.getId())
                .taskId(updated.getTaskId())
                .scheduledAt(updated.getScheduledAt())
                .scheduleStatus(updated.getStatus())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewTaskResponse updateTaskNoteUrl(Long userId, Long taskId, UpdateTaskNoteUrlRequest request) {
        ReviewTask task = reviewTaskMapper.findByIdAndUserId(taskId, userId);
        I18nPreconditions.checkNotNull(task, "error.task.notFound", taskId);
        Integer st = task.getStatus();
        I18nPreconditions.checkState(!Integer.valueOf(ReviewTaskStatus.ARCHIVED.code()).equals(st),
                "error.task.archived.cannotEditNoteUrl");
        I18nPreconditions.checkState(!Integer.valueOf(ReviewTaskStatus.DELETED.code()).equals(st),
                "error.task.deleted.cannotEditNoteUrl");

        String noteUrl = request.getNoteUrl();
        if (noteUrl != null && noteUrl.isBlank()) {
            noteUrl = null;
        }
        reviewTaskMapper.updateNoteUrlByIdAndUserId(taskId, userId, noteUrl);
        task.setNoteUrl(noteUrl);
        return toResponse(task, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewTaskResponse updateTaskNoteContent(Long userId, Long taskId, UpdateTaskNoteContentRequest request) {
        ReviewTask task = reviewTaskMapper.findByIdAndUserId(taskId, userId);
        I18nPreconditions.checkNotNull(task, "error.task.notFound", taskId);
        Integer st = task.getStatus();
        I18nPreconditions.checkState(!Integer.valueOf(ReviewTaskStatus.ARCHIVED.code()).equals(st),
                "error.task.archived.cannotEditContent");
        I18nPreconditions.checkState(!Integer.valueOf(ReviewTaskStatus.DELETED.code()).equals(st),
                "error.task.deleted.cannotEditContent");

        String noteContent = request.getNoteContent();
        if (noteContent != null && noteContent.isBlank()) {
            noteContent = null;
        }
        reviewTaskMapper.updateNoteContentByIdAndUserId(taskId, userId, noteContent);
        task.setNoteContent(noteContent);
        return toResponse(task, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EditReviewTaskResponse editReviewTask(Long userId, Long taskId, EditReviewTaskRequest request) {
        ReviewTask task = reviewTaskMapper.findByIdAndUserId(taskId, userId);
        I18nPreconditions.checkNotNull(task, "error.task.notFound", taskId);
        Integer st = task.getStatus();
        I18nPreconditions.checkState(!Integer.valueOf(ReviewTaskStatus.ARCHIVED.code()).equals(st),
                "error.task.archived.cannotEdit");
        I18nPreconditions.checkState(!Integer.valueOf(ReviewTaskStatus.DELETED.code()).equals(st),
                "error.task.deleted.cannotEdit");

        // 更新任务链接（null 表示不修改）
        if (request.getNoteUrl() != null) {
            String noteUrl = request.getNoteUrl().isBlank() ? null : request.getNoteUrl().trim();
            reviewTaskMapper.updateNoteUrlByIdAndUserId(taskId, userId, noteUrl);
            task.setNoteUrl(noteUrl);
        }

        // 更新复习内容（null 表示不修改）
        if (request.getNoteContent() != null) {
            String noteContent = request.getNoteContent().isBlank() ? null : request.getNoteContent();
            reviewTaskMapper.updateNoteContentByIdAndUserId(taskId, userId, noteContent);
            task.setNoteContent(noteContent);
        }

        // 更新提醒时间（scheduleId 和 scheduledAt 均不为 null 时才修改）
        EditReviewTaskResponse.EditReviewTaskResponseBuilder builder = EditReviewTaskResponse.builder()
                .id(task.getId())
                .userId(task.getUserId())
                .title(task.getTitle())
                .sourceType(task.getSourceType())
                .noteUrl(task.getNoteUrl())
                .noteContent(task.getNoteContent())
                .timezone(task.getTimezone())
                .scheduleMode(task.getScheduleMode())
                .reminderStrategy(task.getReminderStrategy())
                .status(task.getStatus());

        if (request.getScheduleId() != null && request.getScheduledAt() != null) {
            ReminderSchedule schedule = reminderScheduleMapper.findByIdAndUserId(request.getScheduleId(), userId);
            I18nPreconditions.checkArgument(schedule != null && taskId.equals(schedule.getTaskId()),
                    "error.schedule.notFoundOrNotBelongs");
            I18nPreconditions.checkState(!Integer.valueOf(ReminderScheduleStatus.CANCELLED.code()).equals(schedule.getStatus()),
                    "error.schedule.cancelled.cannotEditReminderTime");
            I18nPreconditions.checkState(!Integer.valueOf(ReminderScheduleStatus.SENDING.code()).equals(schedule.getStatus()),
                    "error.schedule.sending.cannotEditReminderTime");
            I18nPreconditions.checkState(reviewRecordMapper.countByUserIdAndScheduleId(userId, request.getScheduleId()) == 0,
                    "error.schedule.reviewCompleted.cannotEditReminderTime");

            validateScheduledAtForNewReminder(request.getScheduledAt());

            reminderScheduleMapper.updateScheduledAtByIdAndUserId(
                    request.getScheduleId(), userId, request.getScheduledAt());

            List<ReminderSchedule> remains = reminderScheduleMapper.findByTaskId(taskId);
            LocalDateTime next = remains.stream()
                    .filter(s -> Integer.valueOf(ReminderScheduleStatus.PENDING.code()).equals(s.getStatus()))
                    .map(ReminderSchedule::getScheduledAt)
                    .min(Comparator.naturalOrder())
                    .orElse(null);
            reviewTaskMapper.updateNextRemindAt(taskId, next);

            ReminderSchedule updated = reminderScheduleMapper.findByIdAndUserId(request.getScheduleId(), userId);
            I18nPreconditions.checkNotNull(updated, "error.schedule.reloadAfterUpdateFailed");
            builder.scheduleId(updated.getId())
                    .scheduledAt(updated.getScheduledAt())
                    .scheduleStatus(updated.getStatus());
        }

        return builder.build();
    }

    private ReviewTask ensureTaskExistsForUser(Long taskId, Long userId) {
        ReviewTask task = reviewTaskMapper.findByIdAndUserId(taskId, userId);
        I18nPreconditions.checkNotNull(task, "error.task.notFound", taskId);
        return task;
    }

    private int resolveReminderStrategy(CreateReviewTaskRequest request, Integer scheduleModeInput) {
        if (request.getReminderStrategy() != null) {
            return ReminderStrategy.fromCode(request.getReminderStrategy()).code();
        }
        if (Integer.valueOf(ScheduleMode.MANUAL.code()).equals(scheduleModeInput)
                && request.getRemindTimes() != null
                && !request.getRemindTimes().isEmpty()) {
            return ReminderStrategy.FULL_CUSTOM.code();
        }
        return ReminderStrategy.FORGETTING_CURVE.code();
    }

    private void validateReminderStrategyRequest(CreateReviewTaskRequest request, int reminderStrategyCode) {
        if (reminderStrategyCode == ReminderStrategy.FULL_CUSTOM.code()) {
            I18nPreconditions.checkArgument(request.getRemindTimes() != null && !request.getRemindTimes().isEmpty(),
                    "error.remindTimes.requiredWhenFullCustom");
            for (LocalDateTime t : request.getRemindTimes()) {
                I18nPreconditions.checkNotNull(t, "error.remindTimes.containsNullEntry");
            }
            return;
        }
        I18nPreconditions.checkArgument(request.getRemindTimes() == null || request.getRemindTimes().isEmpty(),
                "error.remindTimes.forbiddenWhenForgettingCurve");
        if (request.getFirstReminderAt() != null) {
            validateScheduledAtForNewReminder(request.getFirstReminderAt());
        }
    }

    /**
     * 新建排期或首次自定义提醒：不可早于当前时刻，且不可落在当前分钟（与修改排期接口一致）。
     */
    private void validateScheduledAtForNewReminder(LocalDateTime scheduledAt) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentMinute = now.truncatedTo(ChronoUnit.MINUTES);
        I18nPreconditions.checkState(!scheduledAt.truncatedTo(ChronoUnit.MINUTES).equals(currentMinute),
                "error.reminder.mustAfterCurrentMinute");
        I18nPreconditions.checkState(!scheduledAt.isBefore(now), "error.reminder.cannotBeInPast");
    }

    private List<LocalDateTime> buildRemindTimes(CreateReviewTaskRequest request, int reminderStrategyCode, String timezone) {
        if (reminderStrategyCode == ReminderStrategy.FULL_CUSTOM.code()) {
            List<LocalDateTime> list = new ArrayList<>(request.getRemindTimes());
            list.sort(LocalDateTime::compareTo);
            return list;
        }

        ZoneId zone = resolveZoneId(timezone);
        LocalDate d0 = LocalDate.now(zone);
        LocalTime curveTime = request.getCurveRemindTime() != null
                ? request.getCurveRemindTime()
                : forgettingCurveProperties.getEffectiveRemindTime();
        List<Integer> offsets = forgettingCurveProperties.getEffectiveDayOffsets();
        I18nPreconditions.checkState(!offsets.isEmpty(), "error.review.forgetting-curve.empty");
        List<LocalDateTime> curve = new ArrayList<>();
        LocalDateTime first;
        if (request.getFirstReminderAt() != null) {
            first = request.getFirstReminderAt();
        } else {
            first = LocalDateTime.of(d0.plusDays(offsets.get(0)), curveTime);
        }
        curve.add(first);
        for (int i = 1; i < offsets.size(); i++) {
            curve.add(LocalDateTime.of(d0.plusDays(offsets.get(i)), curveTime));
        }
        curve.sort(Comparator.naturalOrder());
        return curve;
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

    private void saveAttachments(Long taskId, Long userId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        I18nPreconditions.checkArgument(files.size() <= storageProperties.getMaxFileCount(),
                "error.attachment.maxCount", storageProperties.getMaxFileCount());
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            I18nPreconditions.checkArgument(file.getSize() <= storageProperties.getMaxFileSizeBytes(),
                    "error.attachment.maxFileSize", storageProperties.getMaxFileSizeBytes());
            TaskAttachmentStorageService.StoredAttachment stored = taskAttachmentStorageService.save(file, userId, taskId);
            ReviewTaskAttachment attachment = new ReviewTaskAttachment();
            attachment.setTaskId(taskId);
            attachment.setUserId(userId);
            attachment.setOriginalFileName(file.getOriginalFilename());
            attachment.setStoredFileName(stored.storedFileName());
            attachment.setContentType(file.getContentType());
            attachment.setFileSize(file.getSize());
            attachment.setFileType(resolveFileType(file.getContentType()));
            attachment.setStoragePath(stored.storagePath());
            attachment.setFileUrl(stored.fileUrl());
            reviewTaskAttachmentMapper.insert(attachment);
        }
    }

    private static int resolveFileType(String contentType) {
        if (contentType != null && contentType.toLowerCase().startsWith("image/")) {
            return 1;
        }
        return 2;
    }

    private ReviewTaskResponse toResponse(ReviewTask task, Long userId) {
        List<ReviewTaskAttachment> attachments = reviewTaskAttachmentMapper.findByTaskIdAndUserId(task.getId(), userId);
        return ReviewTaskResponse.builder()
                .id(task.getId())
                .userId(task.getUserId())
                .title(task.getTitle())
                .sourceType(task.getSourceType())
                .noteUrl(task.getNoteUrl())
                .noteContent(task.getNoteContent())
                .timezone(task.getTimezone())
                .scheduleMode(task.getScheduleMode())
                .reminderStrategy(task.getReminderStrategy())
                .status(task.getStatus())
                .attachments(attachments.stream().map(item -> TaskAttachmentResponse.builder()
                        .id(item.getId())
                        .originalFileName(item.getOriginalFileName())
                        .contentType(item.getContentType())
                        .fileSize(item.getFileSize())
                        .fileType(item.getFileType())
                        .fileUrl(cosAttachmentUrlService.publicUrlForResponse(item))
                        .build()).toList())
                .build();
    }
}
