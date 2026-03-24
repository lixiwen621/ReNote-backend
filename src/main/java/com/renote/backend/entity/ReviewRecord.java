package com.renote.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewRecord {
    private Long id;
    private Long taskId;
    private Long userId;
    private Long scheduleId;
    private LocalDateTime reviewedAt;
    private Integer reviewResult;
    private Integer confidenceScore;
    private String note;
}
