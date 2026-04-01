package com.renote.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateScheduleTimeRequest {
    @NotNull(message = "scheduledAt不能为空")
    private LocalDateTime scheduledAt;
}
