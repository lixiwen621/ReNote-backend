package com.renote.backend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TodayReviewTaskCardResponse {
    private Long taskId;
    private String title;
    private LocalDateTime scheduledAt;
    private Long scheduleId;
    /** 数据库 reminder_schedule.status：1~5 */
    private Integer scheduleStatus;
    /**
     * 通知阶段（便于前端文案）：1=待发送通知 2=发送中 3=已发送通知 4=发送失败（仍可完成复习）
     */
    private Integer reminderNotifyPhase;
    /** 是否可展示「完成复习」；列表内未完成复习时为 true */
    private Boolean canComplete;
}

