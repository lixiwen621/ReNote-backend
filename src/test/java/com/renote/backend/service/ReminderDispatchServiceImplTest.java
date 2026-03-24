package com.renote.backend.service;

import com.renote.backend.entity.ReminderSchedule;
import com.renote.backend.entity.ReviewTask;
import com.renote.backend.mapper.NotifyMessageLogMapper;
import com.renote.backend.mapper.ReminderScheduleMapper;
import com.renote.backend.mapper.ReviewTaskMapper;
import com.renote.backend.notify.WeChatNotifyClient;
import com.renote.backend.service.impl.ReminderDispatchServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReminderDispatchServiceImplTest {

    @Test
    void shouldDispatchPendingReminderSuccessfully() {
        ReminderScheduleMapper scheduleMapper = mock(ReminderScheduleMapper.class);
        ReviewTaskMapper taskMapper = mock(ReviewTaskMapper.class);
        NotifyMessageLogMapper logMapper = mock(NotifyMessageLogMapper.class);
        WeChatNotifyClient client = mock(WeChatNotifyClient.class);
        ReminderDispatchServiceImpl service = new ReminderDispatchServiceImpl(scheduleMapper, taskMapper, logMapper, client);

        ReminderSchedule schedule = new ReminderSchedule();
        schedule.setId(1L);
        schedule.setTaskId(1001L);
        schedule.setUserId(1L);
        schedule.setScheduledAt(LocalDateTime.now().minusMinutes(1));
        schedule.setAttemptCount(0);
        schedule.setMaxAttempts(3);
        when(scheduleMapper.findDuePending(any(), anyInt())).thenReturn(List.of(schedule));
        when(scheduleMapper.markSendingIfPending(anyLong())).thenReturn(1);

        ReviewTask task = new ReviewTask();
        task.setId(1001L);
        task.setUserId(1L);
        task.setTitle("测试提醒");
        task.setNoteUrl("https://example.com");
        when(taskMapper.findById(1001L)).thenReturn(task);
        when(client.sendReminder(anyLong(), any(), any())).thenReturn(com.renote.backend.notify.NotifyResult.success("REQ-1"));

        service.dispatchDueReminders();

        verify(scheduleMapper).markSent(1L);
        verify(logMapper).insert(any());
    }
}
