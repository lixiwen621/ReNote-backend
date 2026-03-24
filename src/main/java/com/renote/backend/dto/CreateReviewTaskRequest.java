package com.renote.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateReviewTaskRequest {

    private Long userId;

    @NotBlank(message = "title不能为空")
    private String title;

    @NotBlank(message = "sourceType不能为空")
    private String sourceType;

    private String noteUrl;
    private String noteContent;
    private String timezone;
    private String scheduleMode;
}
