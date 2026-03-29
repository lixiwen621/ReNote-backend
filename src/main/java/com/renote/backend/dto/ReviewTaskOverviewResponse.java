package com.renote.backend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewTaskOverviewResponse {
    /**
     * 今日待复习任务数：当日仍有未完成复习的排期所涉及的去重 taskId（发通知后仍计入，直至完成复习）
     */
    private Integer dueTaskCount;

    /**
     * 今日未完成复习的排期条数（与 GET /today 列表长度一致；含已发通知但未点完成的条目）
     */
    private Integer dueReminderCount;

    /**
     * 上述未完成复习排期中，仍处于待调度发送（reminder_schedule.status=pending）的条数，用于「待提醒」展示
     */
    private Integer pendingNotifyReminderCount;

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

