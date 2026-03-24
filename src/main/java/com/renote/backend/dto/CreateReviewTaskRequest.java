package com.renote.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CreateReviewTaskRequest {

    private Long userId;

    @NotBlank(message = "title不能为空")
    private String title;

    @NotNull(message = "sourceType不能为空")
    private Integer sourceType;

    private String noteUrl;
    private String noteContent;
    private String timezone;
    private Integer scheduleMode;
    private List<LocalDateTime> remindTimes;
}
