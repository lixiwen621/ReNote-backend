package com.renote.backend.service.impl;

import com.renote.backend.dto.ReviewTaskOverviewResponse;
import com.renote.backend.dto.TodayReviewTaskCardResponse;
import com.renote.backend.dto.WeekReviewDayResponse;
import com.renote.backend.dto.WeekReviewScheduleResponse;
import com.renote.backend.dto.WeekReviewTaskCardResponse;
import com.renote.backend.entity.ReminderSchedule;
import com.renote.backend.entity.ReviewTask;
import com.renote.backend.enums.ReminderScheduleStatus;
import com.renote.backend.mapper.ReminderScheduleMapper;
import com.renote.backend.mapper.ReviewRecordMapper;
import com.renote.backend.mapper.ReviewTaskMapper;
import com.renote.backend.service.ReviewOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewOverviewServiceImpl implements ReviewOverviewService {

    private final ReminderScheduleMapper reminderScheduleMapper;
    private final ReviewTaskMapper reviewTaskMapper;
    private final ReviewRecordMapper reviewRecordMapper;

    @Override
    public ReviewTaskOverviewResponse getTodayOverview(Long userId, LocalDate date) {
        // 今日未完成复习：当天排期且尚无该 schedule_id 的 review_record（通知已发 sent 仍算待复习）
        List<ReminderSchedule> incomplete = reminderScheduleMapper.findIncompleteReviewOnDateByUser(userId, date);
        int dueReminderCount = incomplete.size();
        int dueTaskCount = (int) incomplete.stream()
                .map(ReminderSchedule::getTaskId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        int pendingNotifyReminderCount = (int) incomplete.stream()
                .filter(s -> Integer.valueOf(ReminderScheduleStatus.PENDING.code()).equals(s.getStatus()))
                .count();

        // 今日已完成：按排期 schedule_id 去重；无 schedule_id 的 legacy 记录按 task_id 同日合并计 1
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
        response.setPendingNotifyReminderCount(pendingNotifyReminderCount);
        response.setCompletedTodayCount(completedTodayCount);
        response.setNextUp(nextUp);
        return response;
    }

    @Override
    public List<TodayReviewTaskCardResponse> getTodayTaskCards(Long userId, LocalDate date) {
        List<ReminderSchedule> schedules = reminderScheduleMapper.findIncompleteReviewOnDateByUser(userId, date);
        Map<Long, String> taskTitleCache = new HashMap<>();
        return schedules.stream().map(schedule -> {
            TodayReviewTaskCardResponse card = new TodayReviewTaskCardResponse();
            card.setTaskId(schedule.getTaskId());
            card.setScheduleId(schedule.getId());
            card.setScheduledAt(schedule.getScheduledAt());
            card.setScheduleStatus(schedule.getStatus());
            card.setReminderNotifyPhase(toReminderNotifyPhase(schedule.getStatus()));
            card.setCanComplete(Boolean.TRUE);

            String title = taskTitleCache.computeIfAbsent(schedule.getTaskId(), taskId -> {
                ReviewTask task = reviewTaskMapper.findByIdAndUserId(taskId, userId);
                return task == null ? null : task.getTitle();
            });
            card.setTitle(title);
            return card;
        }).toList();
    }

    @Override
    public WeekReviewScheduleResponse getWeekSchedule(Long userId, LocalDate anchorDate) {
        LocalDate anchor = anchorDate != null ? anchorDate : LocalDate.now();
        LocalDate weekStart = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);

        List<ReminderSchedule> schedules =
                reminderScheduleMapper.findSchedulesInDateRangeByUser(userId, weekStart, weekEnd);
        List<Long> scheduleIds = schedules.stream().map(ReminderSchedule::getId).filter(Objects::nonNull).toList();
        Set<Long> completedIds;
        if (scheduleIds.isEmpty()) {
            completedIds = Set.of();
        } else {
            completedIds = new HashSet<>(reviewRecordMapper.findCompletedScheduleIdsByUserAndScheduleIds(userId, scheduleIds));
        }

        Map<Long, String> taskTitleCache = new HashMap<>();
        Map<LocalDate, List<ReminderSchedule>> byDay =
                schedules.stream().collect(Collectors.groupingBy(s -> s.getScheduledAt().toLocalDate()));

        List<WeekReviewDayResponse> days = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) {
            LocalDate d = weekStart.plusDays(i);
            WeekReviewDayResponse day = new WeekReviewDayResponse();
            day.setDate(d);
            List<ReminderSchedule> daySchedules = new ArrayList<>(byDay.getOrDefault(d, List.of()));
            daySchedules.sort(Comparator.comparing(ReminderSchedule::getScheduledAt));
            day.setItems(daySchedules.stream()
                    .map(s -> toWeekCard(s, userId, taskTitleCache, completedIds))
                    .toList());
            days.add(day);
        }

        WeekReviewScheduleResponse response = new WeekReviewScheduleResponse();
        response.setWeekStart(weekStart);
        response.setWeekEnd(weekEnd);
        response.setDays(days);
        return response;
    }

    private WeekReviewTaskCardResponse toWeekCard(ReminderSchedule schedule,
                                                  Long userId,
                                                  Map<Long, String> taskTitleCache,
                                                  Set<Long> completedIds) {
        boolean reviewCompleted = completedIds.contains(schedule.getId());
        WeekReviewTaskCardResponse card = new WeekReviewTaskCardResponse();
        card.setTaskId(schedule.getTaskId());
        card.setScheduleId(schedule.getId());
        card.setScheduledAt(schedule.getScheduledAt());
        card.setScheduleStatus(schedule.getStatus());
        card.setReminderNotifyPhase(toReminderNotifyPhase(schedule.getStatus()));
        card.setReviewCompleted(reviewCompleted);
        card.setCanComplete(!reviewCompleted);

        String title = taskTitleCache.computeIfAbsent(schedule.getTaskId(), taskId -> {
            ReviewTask task = reviewTaskMapper.findByIdAndUserId(taskId, userId);
            return task == null ? null : task.getTitle();
        });
        card.setTitle(title);
        return card;
    }

    /**
     * 与 reminder_schedule.status 对齐的展示阶段：pending/sending/sent/failed → 1~4
     */
    private static int toReminderNotifyPhase(Integer status) {
        if (status == null) {
            return 1;
        }
        if (status == ReminderScheduleStatus.PENDING.code()) {
            return 1;
        }
        if (status == ReminderScheduleStatus.SENDING.code()) {
            return 2;
        }
        if (status == ReminderScheduleStatus.SENT.code()) {
            return 3;
        }
        if (status == ReminderScheduleStatus.FAILED.code()) {
            return 4;
        }
        return 1;
    }
}

