package com.renote.backend.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class WeekReviewDayResponse {
    private LocalDate date;
    private List<WeekReviewTaskCardResponse> items;
}
