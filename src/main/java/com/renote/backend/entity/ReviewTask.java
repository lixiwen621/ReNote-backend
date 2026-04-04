package com.renote.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewTask {
    private Long id;
    private Long userId;
    private String title;
    private Integer sourceType;
    private String noteUrl;
    private String noteContent;
    private String timezone;
    private Integer scheduleMode;
    /** 提醒时间策略：1=全部自定义 2=遗忘曲线（见 ReminderStrategy） */
    private Integer reminderStrategy;
    private Integer status;
    private LocalDateTime lastReviewedAt;
    private LocalDateTime nextRemindAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
