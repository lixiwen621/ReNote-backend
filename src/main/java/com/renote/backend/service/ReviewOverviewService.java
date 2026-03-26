package com.renote.backend.service;

import com.renote.backend.dto.ReviewTaskOverviewResponse;
import com.renote.backend.dto.TodayReviewTaskCardResponse;

import java.time.LocalDate;
import java.util.List;

public interface ReviewOverviewService {

    ReviewTaskOverviewResponse getTodayOverview(Long userId, LocalDate date);

    List<TodayReviewTaskCardResponse> getTodayTaskCards(Long userId, LocalDate date);
}

