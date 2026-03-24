package com.renote.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReminderSchedule {
    private Long id;
    private Long taskId;
    private Long userId;
    private LocalDateTime scheduledAt;
    private Integer status;
    private Integer attemptCount;
    private Integer maxAttempts;
    private LocalDateTime lastAttemptAt;
    private LocalDateTime sentAt;
    private String idempotencyKey;
    private String failReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
