package com.renote.backend.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EditReviewTaskRequest {

    /** 排期 ID，与 scheduledAt 同时传入时修改提醒时间；不传则不修改提醒时间 */
    private Long scheduleId;

    /** 新的提醒时间（与 scheduleId 配套），不传则不修改 */
    private LocalDateTime scheduledAt;

    /**
     * 任务链接：null = 不修改，"" 或空白 = 清空，非空字符串 = 更新。
     */
    @Size(max = 1024, message = "任务链接不能超过1024个字符")
    private String noteUrl;

    /**
     * 复习内容（HTML）：null = 不修改，空白 = 清空，非空字符串 = 更新。
     */
    @Size(max = 500000, message = "复习内容不能超过500000个字符")
    private String noteContent;
}
