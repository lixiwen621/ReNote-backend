package com.renote.backend.service;

import com.renote.backend.dto.CreateReviewTaskRequest;
import com.renote.backend.dto.ReviewCompleteRequest;
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
        req.setUserId(1L);
        req.setTitle("测试笔记");
        req.setSourceType(1);

        assertNotNull(service.createTask(req));
        verify(reminderScheduleMapper).insert(any());
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
        when(reviewTaskMapper.findById(anyLong())).thenReturn(task);
        when(reminderScheduleMapper.findByTaskId(anyLong())).thenReturn(Collections.emptyList());

        ReviewCompleteRequest req = new ReviewCompleteRequest();
        req.setReviewResult(1);
        req.setConfidenceScore(5);

        service.completeReview(1001L, req);

        ArgumentCaptor<Long> taskIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(reviewTaskMapper).updateLastReviewedAt(taskIdCaptor.capture(), any());
        assertEquals(1001L, taskIdCaptor.getValue());
        verify(reviewRecordMapper).insert(any());
    }
}
