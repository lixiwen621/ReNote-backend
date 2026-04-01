package com.renote.backend.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class WeekReviewScheduleResponse {
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private List<WeekReviewDayResponse> days;
}
