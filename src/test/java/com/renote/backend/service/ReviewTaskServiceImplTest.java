package com.renote.backend.service;

import com.renote.backend.dto.CreateReviewTaskRequest;
import com.renote.backend.dto.ReviewCompleteRequest;
import com.renote.backend.dto.UpdateScheduleTimeRequest;
import com.renote.backend.dto.UpdateTaskNoteUrlRequest;
import com.renote.backend.dto.ReviewTaskResponse;
import com.renote.backend.entity.ReminderSchedule;
import com.renote.backend.entity.ReviewTask;
import com.renote.backend.config.ForgettingCurveProperties;
import com.renote.backend.enums.ReminderScheduleStatus;
import com.renote.backend.enums.ReviewTaskStatus;
import com.renote.backend.mapper.ReminderScheduleMapper;
import com.renote.backend.mapper.ReviewRecordMapper;
import com.renote.backend.mapper.ReviewTaskMapper;
import com.renote.backend.service.impl.ReviewTaskServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewTaskServiceImplTest {

    private static final ForgettingCurveProperties DEFAULT_FORGETTING_CURVE = new ForgettingCurveProperties();

    @Test
    void shouldCreateTaskAndGenerateDefaultSchedules() {
        ReviewTaskMapper reviewTaskMapper = mock(ReviewTaskMapper.class);
        ReminderScheduleMapper reminderScheduleMapper = mock(ReminderScheduleMapper.class);
        ReviewRecordMapper reviewRecordMapper = mock(ReviewRecordMapper.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskMapper, reminderScheduleMapper, reviewRecordMapper, DEFAULT_FORGETTING_CURVE);

        doAnswer(invocation -> {
            ReviewTask task = invocation.getArgument(0);
            task.setId(1001L);
            return 1;
        }).when(reviewTaskMapper).insert(any(ReviewTask.class));

        CreateReviewTaskRequest req = new CreateReviewTaskRequest();
        req.setTitle("测试笔记");
        req.setSourceType(1);

        assertNotNull(service.createTask(1L, req));
        // 默认遗忘曲线会生成 6 个提醒时间点（1,2,4,7,15,20 天）
        verify(reminderScheduleMapper, org.mockito.Mockito.times(6)).insert(any());
    }

    @Test
    void forgettingCurveSchedulesUseFixed1430WallClock() {
        ReviewTaskMapper reviewTaskMapper = mock(ReviewTaskMapper.class);
        ReminderScheduleMapper reminderScheduleMapper = mock(ReminderScheduleMapper.class);
        ReviewRecordMapper reviewRecordMapper = mock(ReviewRecordMapper.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskMapper, reminderScheduleMapper, reviewRecordMapper, DEFAULT_FORGETTING_CURVE);

        doAnswer(invocation -> {
            ReviewTask task = invocation.getArgument(0);
            task.setId(1001L);
            return 1;
        }).when(reviewTaskMapper).insert(any(ReviewTask.class));

        CreateReviewTaskRequest req = new CreateReviewTaskRequest();
        req.setTitle("测试笔记");
        req.setSourceType(1);
        req.setTimezone("Asia/Shanghai");

        ArgumentCaptor<ReminderSchedule> captor = ArgumentCaptor.forClass(ReminderSchedule.class);

        service.createTask(1L, req);

        verify(reminderScheduleMapper, times(6)).insert(captor.capture());
        List<ReminderSchedule> inserted = captor.getAllValues();
        for (ReminderSchedule s : inserted) {
            assertEquals(LocalTime.of(14, 30), s.getScheduledAt().toLocalTime());
        }
    }

    @Test
    void shouldCompleteReviewAndUpdateTask() {
        ReviewTaskMapper reviewTaskMapper = mock(ReviewTaskMapper.class);
        ReminderScheduleMapper reminderScheduleMapper = mock(ReminderScheduleMapper.class);
        ReviewRecordMapper reviewRecordMapper = mock(ReviewRecordMapper.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskMapper, reminderScheduleMapper, reviewRecordMapper, DEFAULT_FORGETTING_CURVE);

        ReviewTask task = new ReviewTask();
        task.setId(1001L);
        task.setUserId(1L);
        when(reviewTaskMapper.findByIdAndUserId(anyLong(), anyLong())).thenReturn(task);
        when(reminderScheduleMapper.findByTaskId(anyLong())).thenReturn(Collections.emptyList());

        ReviewCompleteRequest req = new ReviewCompleteRequest();
        req.setReviewResult(1);
        req.setConfidenceScore(5);

        service.completeReview(1L, 1001L, req);

        ArgumentCaptor<Long> taskIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(reviewTaskMapper).updateLastReviewedAt(taskIdCaptor.capture(), any());
        assertEquals(1001L, taskIdCaptor.getValue());
        verify(reviewRecordMapper).insert(any());
        verify(reminderScheduleMapper, never()).findByIdAndUserId(anyLong(), anyLong());
        verify(reminderScheduleMapper, never()).markSentIfPending(anyLong());
    }

    @Test
    void shouldCompleteWithScheduleIdAndMarkScheduleSent() {
        ReviewTaskMapper reviewTaskMapper = mock(ReviewTaskMapper.class);
        ReminderScheduleMapper reminderScheduleMapper = mock(ReminderScheduleMapper.class);
        ReviewRecordMapper reviewRecordMapper = mock(ReviewRecordMapper.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskMapper, reminderScheduleMapper, reviewRecordMapper, DEFAULT_FORGETTING_CURVE);

        ReviewTask task = new ReviewTask();
        task.setId(1001L);
        task.setUserId(1L);
        when(reviewTaskMapper.findByIdAndUserId(anyLong(), anyLong())).thenReturn(task);

        ReminderSchedule schedule = new ReminderSchedule();
        schedule.setId(2001L);
        schedule.setTaskId(1001L);
        schedule.setUserId(1L);
        when(reminderScheduleMapper.findByIdAndUserId(2001L, 1L)).thenReturn(schedule);
        when(reviewRecordMapper.countByUserIdAndScheduleId(1L, 2001L)).thenReturn(0);
        when(reminderScheduleMapper.findByTaskId(1001L)).thenReturn(Collections.emptyList());

        ReviewCompleteRequest req = new ReviewCompleteRequest();
        req.setScheduleId(2001L);
        req.setReviewResult(1);

        service.completeReview(1L, 1001L, req);

        verify(reviewRecordMapper, times(1)).insert(any());
        verify(reminderScheduleMapper, times(1)).markSentIfPending(2001L);
    }

    @Test
    void shouldNotInsertSecondRecordWhenSameScheduleIdCompletedTwice() {
        ReviewTaskMapper reviewTaskMapper = mock(ReviewTaskMapper.class);
        ReminderScheduleMapper reminderScheduleMapper = mock(ReminderScheduleMapper.class);
        ReviewRecordMapper reviewRecordMapper = mock(ReviewRecordMapper.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskMapper, reminderScheduleMapper, reviewRecordMapper, DEFAULT_FORGETTING_CURVE);

        ReviewTask task = new ReviewTask();
        task.setId(1001L);
        task.setUserId(1L);
        when(reviewTaskMapper.findByIdAndUserId(anyLong(), anyLong())).thenReturn(task);

        ReminderSchedule schedule = new ReminderSchedule();
        schedule.setId(2001L);
        schedule.setTaskId(1001L);
        schedule.setUserId(1L);
        when(reminderScheduleMapper.findByIdAndUserId(2001L, 1L)).thenReturn(schedule);
        when(reviewRecordMapper.countByUserIdAndScheduleId(1L, 2001L)).thenReturn(0, 1);
        when(reminderScheduleMapper.findByTaskId(1001L)).thenReturn(Collections.emptyList());

        ReviewCompleteRequest req = new ReviewCompleteRequest();
        req.setScheduleId(2001L);
        req.setReviewResult(1);

        service.completeReview(1L, 1001L, req);
        service.completeReview(1L, 1001L, req);

        verify(reviewRecordMapper, times(1)).insert(any());
        verify(reviewTaskMapper, times(1)).updateLastReviewedAt(anyLong(), any());
        verify(reminderScheduleMapper, times(2)).markSentIfPending(2001L);
    }

    @Test
    void shouldRejectUpdateWhenScheduleCancelled() {
        ReviewTaskMapper reviewTaskMapper = mock(ReviewTaskMapper.class);
        ReminderScheduleMapper reminderScheduleMapper = mock(ReminderScheduleMapper.class);
        ReviewRecordMapper reviewRecordMapper = mock(ReviewRecordMapper.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskMapper, reminderScheduleMapper, reviewRecordMapper, DEFAULT_FORGETTING_CURVE);

        ReviewTask task = new ReviewTask();
        task.setId(1001L);
        task.setUserId(1L);
        when(reviewTaskMapper.findByIdAndUserId(1001L, 1L)).thenReturn(task);

        ReminderSchedule schedule = new ReminderSchedule();
        schedule.setId(2001L);
        schedule.setTaskId(1001L);
        schedule.setUserId(1L);
        schedule.setStatus(ReminderScheduleStatus.CANCELLED.code());
        when(reminderScheduleMapper.findByIdAndUserId(2001L, 1L)).thenReturn(schedule);

        UpdateScheduleTimeRequest request = new UpdateScheduleTimeRequest();
        request.setScheduledAt(LocalDateTime.now().plusHours(1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateScheduleTime(1L, 1001L, 2001L, request));
        assertEquals("该排期已取消，不可修改", ex.getMessage());
    }

    @Test
    void shouldRejectUpdateWhenScheduleSending() {
        ReviewTaskMapper reviewTaskMapper = mock(ReviewTaskMapper.class);
        ReminderScheduleMapper reminderScheduleMapper = mock(ReminderScheduleMapper.class);
        ReviewRecordMapper reviewRecordMapper = mock(ReviewRecordMapper.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskMapper, reminderScheduleMapper, reviewRecordMapper, DEFAULT_FORGETTING_CURVE);

        ReviewTask task = new ReviewTask();
        task.setId(1001L);
        task.setUserId(1L);
        when(reviewTaskMapper.findByIdAndUserId(1001L, 1L)).thenReturn(task);

        ReminderSchedule schedule = new ReminderSchedule();
        schedule.setId(2001L);
        schedule.setTaskId(1001L);
        schedule.setUserId(1L);
        schedule.setStatus(ReminderScheduleStatus.SENDING.code());
        when(reminderScheduleMapper.findByIdAndUserId(2001L, 1L)).thenReturn(schedule);

        UpdateScheduleTimeRequest request = new UpdateScheduleTimeRequest();
        request.setScheduledAt(LocalDateTime.now().plusHours(1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateScheduleTime(1L, 1001L, 2001L, request));
        assertEquals("发送中排期暂不支持修改，请稍后重试", ex.getMessage());
    }

    @Test
    void shouldRejectUpdateWhenScheduleCompleted() {
        ReviewTaskMapper reviewTaskMapper = mock(ReviewTaskMapper.class);
        ReminderScheduleMapper reminderScheduleMapper = mock(ReminderScheduleMapper.class);
        ReviewRecordMapper reviewRecordMapper = mock(ReviewRecordMapper.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskMapper, reminderScheduleMapper, reviewRecordMapper, DEFAULT_FORGETTING_CURVE);

        ReviewTask task = new ReviewTask();
        task.setId(1001L);
        task.setUserId(1L);
        when(reviewTaskMapper.findByIdAndUserId(1001L, 1L)).thenReturn(task);

        ReminderSchedule schedule = new ReminderSchedule();
        schedule.setId(2001L);
        schedule.setTaskId(1001L);
        schedule.setUserId(1L);
        schedule.setStatus(ReminderScheduleStatus.PENDING.code());
        when(reminderScheduleMapper.findByIdAndUserId(2001L, 1L)).thenReturn(schedule);
        when(reviewRecordMapper.countByUserIdAndScheduleId(1L, 2001L)).thenReturn(1);

        UpdateScheduleTimeRequest request = new UpdateScheduleTimeRequest();
        request.setScheduledAt(LocalDateTime.now().plusHours(1));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateScheduleTime(1L, 1001L, 2001L, request));
        assertEquals("该排期已完成复习，不可修改", ex.getMessage());
    }

    @Test
    void shouldAllowSentButUncompletedAndResetToPending() {
        ReviewTaskMapper reviewTaskMapper = mock(ReviewTaskMapper.class);
        ReminderScheduleMapper reminderScheduleMapper = mock(ReminderScheduleMapper.class);
        ReviewRecordMapper reviewRecordMapper = mock(ReviewRecordMapper.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskMapper, reminderScheduleMapper, reviewRecordMapper, DEFAULT_FORGETTING_CURVE);

        ReviewTask task = new ReviewTask();
        task.setId(1001L);
        task.setUserId(1L);
        when(reviewTaskMapper.findByIdAndUserId(1001L, 1L)).thenReturn(task);

        ReminderSchedule before = new ReminderSchedule();
        before.setId(2001L);
        before.setTaskId(1001L);
        before.setUserId(1L);
        before.setStatus(ReminderScheduleStatus.SENT.code());
        before.setScheduledAt(LocalDateTime.now().minusMinutes(5));

        ReminderSchedule after = new ReminderSchedule();
        after.setId(2001L);
        after.setTaskId(1001L);
        after.setUserId(1L);
        after.setStatus(ReminderScheduleStatus.PENDING.code());
        after.setScheduledAt(LocalDateTime.now().plusHours(2));

        when(reminderScheduleMapper.findByIdAndUserId(2001L, 1L)).thenReturn(before, after);
        when(reviewRecordMapper.countByUserIdAndScheduleId(1L, 2001L)).thenReturn(0);
        when(reminderScheduleMapper.findByTaskId(1001L)).thenReturn(List.of(after));

        UpdateScheduleTimeRequest request = new UpdateScheduleTimeRequest();
        request.setScheduledAt(after.getScheduledAt());

        var resp = service.updateScheduleTime(1L, 1001L, 2001L, request);

        verify(reminderScheduleMapper, times(1))
                .updateScheduledAtByIdAndUserId(2001L, 1L, after.getScheduledAt());
        verify(reviewTaskMapper, times(1)).updateNextRemindAt(1001L, after.getScheduledAt());
        assertEquals(ReminderScheduleStatus.PENDING.code(), resp.getScheduleStatus());
        assertTrue(resp.getScheduledAt().isAfter(LocalDateTime.now().plusMinutes(1)));
    }

    @Test
    void shouldUpdateNoteUrlForActiveTask() {
        ReviewTaskMapper reviewTaskMapper = mock(ReviewTaskMapper.class);
        ReminderScheduleMapper reminderScheduleMapper = mock(ReminderScheduleMapper.class);
        ReviewRecordMapper reviewRecordMapper = mock(ReviewRecordMapper.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskMapper, reminderScheduleMapper, reviewRecordMapper, DEFAULT_FORGETTING_CURVE);

        ReviewTask task = new ReviewTask();
        task.setId(1001L);
        task.setUserId(1L);
        task.setTitle("t");
        task.setSourceType(1);
        task.setTimezone("Asia/Shanghai");
        task.setScheduleMode(1);
        task.setStatus(ReviewTaskStatus.ACTIVE.code());
        when(reviewTaskMapper.findByIdAndUserId(1001L, 1L)).thenReturn(task);
        when(reviewTaskMapper.updateNoteUrlByIdAndUserId(1001L, 1L, "https://x.com/a")).thenReturn(1);

        UpdateTaskNoteUrlRequest req = new UpdateTaskNoteUrlRequest();
        req.setNoteUrl("https://x.com/a");

        ReviewTaskResponse resp = service.updateTaskNoteUrl(1L, 1001L, req);
        assertEquals("https://x.com/a", resp.getNoteUrl());
        verify(reviewTaskMapper).updateNoteUrlByIdAndUserId(1001L, 1L, "https://x.com/a");
    }

    @Test
    void shouldRejectNoteUrlWhenTaskArchived() {
        ReviewTaskMapper reviewTaskMapper = mock(ReviewTaskMapper.class);
        ReminderScheduleMapper reminderScheduleMapper = mock(ReminderScheduleMapper.class);
        ReviewRecordMapper reviewRecordMapper = mock(ReviewRecordMapper.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskMapper, reminderScheduleMapper, reviewRecordMapper, DEFAULT_FORGETTING_CURVE);

        ReviewTask task = new ReviewTask();
        task.setId(1001L);
        task.setUserId(1L);
        task.setStatus(ReviewTaskStatus.ARCHIVED.code());
        when(reviewTaskMapper.findByIdAndUserId(1001L, 1L)).thenReturn(task);

        UpdateTaskNoteUrlRequest req = new UpdateTaskNoteUrlRequest();
        req.setNoteUrl("https://x.com/a");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.updateTaskNoteUrl(1L, 1001L, req));
        assertEquals("任务已归档，不可修改链接", ex.getMessage());
    }

    @Test
    void shouldRejectForgettingCurveWhenRemindTimesProvided() {
        ReviewTaskMapper reviewTaskMapper = mock(ReviewTaskMapper.class);
        ReminderScheduleMapper reminderScheduleMapper = mock(ReminderScheduleMapper.class);
        ReviewRecordMapper reviewRecordMapper = mock(ReviewRecordMapper.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskMapper, reminderScheduleMapper, reviewRecordMapper, DEFAULT_FORGETTING_CURVE);

        CreateReviewTaskRequest req = new CreateReviewTaskRequest();
        req.setTitle("测试");
        req.setSourceType(1);
        req.setReminderStrategy(2);
        req.setRemindTimes(List.of(LocalDateTime.now().plusDays(1)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.createTask(1L, req));
        assertEquals("提醒时间类型为遗忘曲线时，请勿传入remindTimes", ex.getMessage());
    }

    @Test
    void shouldRejectFullCustomWhenRemindTimesEmpty() {
        ReviewTaskMapper reviewTaskMapper = mock(ReviewTaskMapper.class);
        ReminderScheduleMapper reminderScheduleMapper = mock(ReminderScheduleMapper.class);
        ReviewRecordMapper reviewRecordMapper = mock(ReviewRecordMapper.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskMapper, reminderScheduleMapper, reviewRecordMapper, DEFAULT_FORGETTING_CURVE);

        CreateReviewTaskRequest req = new CreateReviewTaskRequest();
        req.setTitle("测试");
        req.setSourceType(1);
        req.setReminderStrategy(1);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.createTask(1L, req));
        assertEquals("提醒时间类型为全部自定义时，remindTimes不能为空", ex.getMessage());
    }
}
