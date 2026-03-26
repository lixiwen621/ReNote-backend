package com.renote.backend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewTaskOverviewResponse {
    /**
     * 今日待复习任务数（去重，按 taskId 去重）
     */
    private Integer dueTaskCount;

    /**
     * 今日待提醒次数（按 schedule 计数）
     */
    private Integer dueReminderCount;

    /**
     * 今日已完成复习次数（按 review_record 计数）
     */
    private Integer completedTodayCount;

    private NextUpTaskResponse nextUp;

    @Data
    public static class NextUpTaskResponse {
        private Long taskId;
        private String title;
        private LocalDateTime nextRemindAt;
    }
}

