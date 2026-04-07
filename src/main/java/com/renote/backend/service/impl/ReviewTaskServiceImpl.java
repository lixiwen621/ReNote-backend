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
import com.renote.backend.config.ForgettingCurveProperties;
import com.renote.backend.service.ReviewTaskService;
import com.renote.backend.config.TaskAttachmentStorageProperties;
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

    public ReviewTaskServiceImpl(ReviewTaskMapper reviewTaskMapper,
                                 ReminderScheduleMapper reminderScheduleMapper,
                                 ReviewRecordMapper reviewRecordMapper,
                                 ReviewTaskAttachmentMapper reviewTaskAttachmentMapper,
                                 ForgettingCurveProperties forgettingCurveProperties,
                                 TaskAttachmentStorageService taskAttachmentStorageService,
                                 TaskAttachmentStorageProperties storageProperties) {
        this.reviewTaskMapper = reviewTaskMapper;
        this.reminderScheduleMapper = reminderScheduleMapper;
        this.reviewRecordMapper = reviewRecordMapper;
        this.reviewTaskAttachmentMapper = reviewTaskAttachmentMapper;
        this.forgettingCurveProperties = forgettingCurveProperties;
        this.taskAttachmentStorageService = taskAttachmentStorageService;
        this.storageProperties = storageProperties;
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
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
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
        if (updated == null) {
            throw new IllegalArgumentException("排期更新后查询失败，请重试");
        }
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
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        Integer st = task.getStatus();
        if (Integer.valueOf(ReviewTaskStatus.ARCHIVED.code()).equals(st)) {
            throw new IllegalArgumentException("任务已归档，不可修改链接");
        }
        if (Integer.valueOf(ReviewTaskStatus.DELETED.code()).equals(st)) {
            throw new IllegalArgumentException("任务已删除，不可修改链接");
        }

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
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        Integer st = task.getStatus();
        if (Integer.valueOf(ReviewTaskStatus.ARCHIVED.code()).equals(st)) {
            throw new IllegalArgumentException("任务已归档，不可修改内容");
        }
        if (Integer.valueOf(ReviewTaskStatus.DELETED.code()).equals(st)) {
            throw new IllegalArgumentException("任务已删除，不可修改内容");
        }

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
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        Integer st = task.getStatus();
        if (Integer.valueOf(ReviewTaskStatus.ARCHIVED.code()).equals(st)) {
            throw new IllegalArgumentException("任务已归档，不可修改");
        }
        if (Integer.valueOf(ReviewTaskStatus.DELETED.code()).equals(st)) {
            throw new IllegalArgumentException("任务已删除，不可修改");
        }

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
            if (schedule == null || !taskId.equals(schedule.getTaskId())) {
                throw new IllegalArgumentException("提醒计划不存在或不属于该任务");
            }
            if (Integer.valueOf(ReminderScheduleStatus.CANCELLED.code()).equals(schedule.getStatus())) {
                throw new IllegalArgumentException("该排期已取消，不可修改提醒时间");
            }
            if (Integer.valueOf(ReminderScheduleStatus.SENDING.code()).equals(schedule.getStatus())) {
                throw new IllegalArgumentException("发送中排期暂不支持修改提醒时间，请稍后重试");
            }
            if (reviewRecordMapper.countByUserIdAndScheduleId(userId, request.getScheduleId()) > 0) {
                throw new IllegalArgumentException("该排期已完成复习，不可修改提醒时间");
            }

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
            if (updated == null) {
                throw new IllegalArgumentException("排期更新后查询失败，请重试");
            }
            builder.scheduleId(updated.getId())
                    .scheduledAt(updated.getScheduledAt())
                    .scheduleStatus(updated.getStatus());
        }

        return builder.build();
    }

    private ReviewTask ensureTaskExistsForUser(Long taskId, Long userId) {
        ReviewTask task = reviewTaskMapper.findByIdAndUserId(taskId, userId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
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
            if (request.getRemindTimes() == null || request.getRemindTimes().isEmpty()) {
                throw new IllegalArgumentException("提醒时间类型为全部自定义时，remindTimes不能为空");
            }
            for (LocalDateTime t : request.getRemindTimes()) {
                if (t == null) {
                    throw new IllegalArgumentException("remindTimes中不能包含空时间");
                }
            }
            return;
        }
        if (request.getRemindTimes() != null && !request.getRemindTimes().isEmpty()) {
            throw new IllegalArgumentException("提醒时间类型为遗忘曲线时，请勿传入remindTimes");
        }
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
        if (scheduledAt.truncatedTo(ChronoUnit.MINUTES).equals(currentMinute)) {
            throw new IllegalArgumentException("提醒时间必须晚于当前分钟");
        }
        if (scheduledAt.isBefore(now)) {
            throw new IllegalArgumentException("提醒时间不能早于当前时间");
        }
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
        if (offsets.isEmpty()) {
            throw new IllegalStateException("遗忘曲线day-offsets配置为空");
        }
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
        if (files.size() > storageProperties.getMaxFileCount()) {
            throw new IllegalArgumentException("单次最多上传" + storageProperties.getMaxFileCount() + "个文件");
        }
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            if (file.getSize() > storageProperties.getMaxFileSizeBytes()) {
                throw new IllegalArgumentException("文件过大，单文件限制" + storageProperties.getMaxFileSizeBytes() + "字节");
            }
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
                        .fileUrl(item.getFileUrl())
                        .build()).toList())
                .build();
    }
}
