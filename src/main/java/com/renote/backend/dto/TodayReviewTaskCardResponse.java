package com.renote.backend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TodayReviewTaskCardResponse {
    private Long taskId;
    private String title;
    private LocalDateTime scheduledAt;
    private Long scheduleId;
    private Integer scheduleStatus;
    private Boolean canComplete;
}

