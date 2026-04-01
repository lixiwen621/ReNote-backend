package com.renote.backend.service;

import com.renote.backend.dto.ReviewTaskOverviewResponse;
import com.renote.backend.dto.TodayReviewTaskCardResponse;
import com.renote.backend.dto.WeekReviewScheduleResponse;

import java.time.LocalDate;
import java.util.List;

public interface ReviewOverviewService {

    ReviewTaskOverviewResponse getTodayOverview(Long userId, LocalDate date);

    List<TodayReviewTaskCardResponse> getTodayTaskCards(Long userId, LocalDate date);

    /**
     * 自然周（周一～周日）复习排期预览；含已完成复习的排期。
     *
     * @param anchorDate 落在目标周内任意一天；用于计算该周的起止
     */
    WeekReviewScheduleResponse getWeekSchedule(Long userId, LocalDate anchorDate);
}

