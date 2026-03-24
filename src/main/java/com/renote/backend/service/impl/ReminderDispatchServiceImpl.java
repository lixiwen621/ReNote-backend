package com.renote.backend.service.impl;

import com.renote.backend.entity.NotifyMessageLog;
import com.renote.backend.entity.ReminderSchedule;
import com.renote.backend.entity.ReviewTask;
import com.renote.backend.enums.NotifyChannel;
import com.renote.backend.enums.NotifySendStatus;
import com.renote.backend.enums.ReminderScheduleStatus;
import com.renote.backend.mapper.NotifyMessageLogMapper;
import com.renote.backend.mapper.ReminderScheduleMapper;
import com.renote.backend.mapper.ReviewTaskMapper;
import com.renote.backend.notify.NotifyResult;
import com.renote.backend.notify.WeChatNotifyClient;
import com.renote.backend.service.ReminderDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReminderDispatchServiceImpl implements ReminderDispatchService {

    private final ReminderScheduleMapper reminderScheduleMapper;
    private final ReviewTaskMapper reviewTaskMapper;
    private final NotifyMessageLogMapper notifyMessageLogMapper;
    private final WeChatNotifyClient weChatNotifyClient;

    @Override
    public void dispatchDueReminders() {
        List<ReminderSchedule> dueSchedules = reminderScheduleMapper.findDuePending(LocalDateTime.now(), 200);
        for (ReminderSchedule schedule : dueSchedules) {
            int locked = reminderScheduleMapper.markSendingIfPending(schedule.getId());
            if (locked == 0) {
                continue;
            }
            processSingle(schedule);
        }
    }

    private void processSingle(ReminderSchedule schedule) {
        ReviewTask task = reviewTaskMapper.findById(schedule.getTaskId());
        if (task == null) {
            reminderScheduleMapper.markFailure(schedule.getId(), ReminderScheduleStatus.FAILED.code(), "任务不存在");
            return;
        }

        String content = task.getNoteUrl() != null ? task.getNoteUrl() : task.getNoteContent();
        NotifyResult result = weChatNotifyClient.sendReminder(task.getUserId(), task.getTitle(), content);

        NotifyMessageLog log = new NotifyMessageLog();
        log.setScheduleId(schedule.getId());
        log.setTaskId(task.getId());
        log.setUserId(task.getUserId());
        log.setChannel(NotifyChannel.WECHAT.code());
        log.setRequestId(result.isSuccess() ? result.getRequestId() : "FAIL-" + UUID.randomUUID());
        log.setMessageTitle(task.getTitle());
        log.setMessageBody(content);
        log.setSentAt(LocalDateTime.now());

        if (result.isSuccess()) {
            reminderScheduleMapper.markSent(schedule.getId());
            log.setSendStatus(NotifySendStatus.SUCCESS.code());
        } else {
            int nextStatus = schedule.getAttemptCount() + 1 >= schedule.getMaxAttempts()
                    ? ReminderScheduleStatus.FAILED.code()
                    : ReminderScheduleStatus.PENDING.code();
            reminderScheduleMapper.markFailure(schedule.getId(), nextStatus, result.getErrorMessage());
            log.setSendStatus(NotifySendStatus.FAILED.code());
            log.setErrorCode(result.getErrorCode());
            log.setErrorMessage(result.getErrorMessage());
        }
        notifyMessageLogMapper.insert(log);
    }
}
