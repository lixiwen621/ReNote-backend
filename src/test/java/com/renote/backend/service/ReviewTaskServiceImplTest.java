package com.renote.backend.service;

import com.renote.backend.dto.CreateReviewTaskRequest;
import com.renote.backend.dto.ReviewCompleteRequest;
import com.renote.backend.entity.ReminderSchedule;
import com.renote.backend.entity.ReviewTask;
import com.renote.backend.mapper.ReminderScheduleMapper;
import com.renote.backend.mapper.ReviewRecordMapper;
import com.renote.backend.mapper.ReviewTaskMapper;
import com.renote.backend.service.impl.ReviewTaskServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewTaskServiceImplTest {

    @Test
    void shouldCreateTaskAndGenerateDefaultSchedules() {
        ReviewTaskMapper reviewTaskMapper = mock(ReviewTaskMapper.class);
        ReminderScheduleMapper reminderScheduleMapper = mock(ReminderScheduleMapper.class);
        ReviewRecordMapper reviewRecordMapper = mock(ReviewRecordMapper.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskMapper, reminderScheduleMapper, reviewRecordMapper);

        doAnswer(invocation -> {
            ReviewTask task = invocation.getArgument(0);
            task.setId(1001L);
            return 1;
        }).when(reviewTaskMapper).insert(any(ReviewTask.class));

        CreateReviewTaskRequest req = new CreateReviewTaskRequest();
        req.setTitle("测试笔记");
        req.setSourceType(1);

        assertNotNull(service.createTask(1L, req));
        // 默认遗忘曲线会生成 5 个提醒时间点
        verify(reminderScheduleMapper, org.mockito.Mockito.times(5)).insert(any());
    }

    @Test
    void shouldCompleteReviewAndUpdateTask() {
        ReviewTaskMapper reviewTaskMapper = mock(ReviewTaskMapper.class);
        ReminderScheduleMapper reminderScheduleMapper = mock(ReminderScheduleMapper.class);
        ReviewRecordMapper reviewRecordMapper = mock(ReviewRecordMapper.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskMapper, reminderScheduleMapper, reviewRecordMapper);

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
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskMapper, reminderScheduleMapper, reviewRecordMapper);

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
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(reviewTaskMapper, reminderScheduleMapper, reviewRecordMapper);

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
}
