package com.renote.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewCompleteRequest {
    private Long userId;
    private Long scheduleId;
    @NotNull(message = "reviewResult不能为空")
    private Integer reviewResult;
    private Integer confidenceScore;
    private String note;
}
