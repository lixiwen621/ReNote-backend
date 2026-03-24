package com.renote.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReminderScheduleResponse {
    private Long id;
    private Long taskId;
    private Long userId;
    private LocalDateTime scheduledAt;
    private Integer status;
    private Integer attemptCount;
    private LocalDateTime sentAt;
    private String failReason;
}
