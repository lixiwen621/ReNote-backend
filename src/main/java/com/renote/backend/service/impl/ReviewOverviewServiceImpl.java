package com.renote.backend.service.impl;

import com.renote.backend.dto.ReviewTaskOverviewResponse;
import com.renote.backend.dto.TodayReviewTaskCardResponse;
import com.renote.backend.entity.ReminderSchedule;
import com.renote.backend.entity.ReviewTask;
import com.renote.backend.mapper.ReminderScheduleMapper;
import com.renote.backend.mapper.ReviewRecordMapper;
import com.renote.backend.mapper.ReviewTaskMapper;
import com.renote.backend.service.ReviewOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ReviewOverviewServiceImpl implements ReviewOverviewService {

    private final ReminderScheduleMapper reminderScheduleMapper;
    private final ReviewTaskMapper reviewTaskMapper;
    private final ReviewRecordMapper reviewRecordMapper;

    @Override
    public ReviewTaskOverviewResponse getTodayOverview(Long userId, LocalDate date) {
        // 今日待提醒明细：当天 reminder_schedule.status=pending(1)
        List<ReminderSchedule> dueSchedules = reminderScheduleMapper.findPendingOnDateByUser(userId, date);
        int dueReminderCount = dueSchedules.size();
        int dueTaskCount = (int) dueSchedules.stream()
                .map(ReminderSchedule::getTaskId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        // 今日已完成复习次数（按 review_record 条数统计）
        int completedTodayCount = reviewRecordMapper.countReviewedOnDateByUser(userId, date);

        // Next up：用户维度最早 pending 提醒计划
        ReminderSchedule nextPending = reminderScheduleMapper.findNextPendingByUser(userId);
        ReviewTaskOverviewResponse.NextUpTaskResponse nextUp = null;
        if (nextPending != null) {
            ReviewTask task = reviewTaskMapper.findByIdAndUserId(nextPending.getTaskId(), userId);
            nextUp = new ReviewTaskOverviewResponse.NextUpTaskResponse();
            nextUp.setTaskId(nextPending.getTaskId());
            nextUp.setTitle(task != null ? task.getTitle() : null);
            nextUp.setNextRemindAt(nextPending.getScheduledAt());
        }

        ReviewTaskOverviewResponse response = new ReviewTaskOverviewResponse();
        response.setDueTaskCount(dueTaskCount);
        response.setDueReminderCount(dueReminderCount);
        response.setCompletedTodayCount(completedTodayCount);
        response.setNextUp(nextUp);
        return response;
    }

    @Override
    public List<TodayReviewTaskCardResponse> getTodayTaskCards(Long userId, LocalDate date) {
        List<ReminderSchedule> schedules = reminderScheduleMapper.findPendingOnDateByUser(userId, date);
        Map<Long, String> taskTitleCache = new HashMap<>();
        return schedules.stream().map(schedule -> {
            TodayReviewTaskCardResponse card = new TodayReviewTaskCardResponse();
            card.setTaskId(schedule.getTaskId());
            card.setScheduleId(schedule.getId());
            card.setScheduledAt(schedule.getScheduledAt());
            card.setScheduleStatus(schedule.getStatus());
            card.setCanComplete(Boolean.TRUE);

            String title = taskTitleCache.computeIfAbsent(schedule.getTaskId(), taskId -> {
                ReviewTask task = reviewTaskMapper.findByIdAndUserId(taskId, userId);
                return task == null ? null : task.getTitle();
            });
            card.setTitle(title);
            return card;
        }).toList();
    }
}

