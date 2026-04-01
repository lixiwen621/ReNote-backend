package com.renote.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UpdateScheduleTimeResponse {
    private Long scheduleId;
    private Long taskId;
    private LocalDateTime scheduledAt;
    private Integer scheduleStatus;
}
