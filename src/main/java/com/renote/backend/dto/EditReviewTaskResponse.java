package com.renote.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EditReviewTaskResponse {

    // ---- 任务字段 ----
    private Long id;
    private Long userId;
    private String title;
    private Integer sourceType;
    private String noteUrl;
    private String noteContent;
    private String timezone;
    private Integer scheduleMode;
    private Integer reminderStrategy;
    private Integer status;

    // ---- 排期字段（仅在修改了提醒时间时有值，否则为 null）----
    private Long scheduleId;
    private LocalDateTime scheduledAt;
    private Integer scheduleStatus;
}
