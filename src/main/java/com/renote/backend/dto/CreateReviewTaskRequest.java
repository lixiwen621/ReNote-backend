package com.renote.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
public class CreateReviewTaskRequest {

    @NotBlank(message = "title不能为空")
    private String title;

    @NotNull(message = "sourceType不能为空")
    private Integer sourceType;

    private String noteUrl;
    private String noteContent;
    private String timezone;
    private Integer scheduleMode;
    /**
     * 提醒时间类型：1=全部自定义（须传 {@code remindTimes}）；2=遗忘曲线（第一次可 {@code firstReminderAt}，其余天按偏移 + {@code curveRemindTime}）。
     * 不传时：若传了非空 {@code remindTimes} 则视为 1，否则视为 2（兼容旧客户端）。
     */
    private Integer reminderStrategy;
    /** 遗忘曲线第一次提醒（可覆盖「创建日+首个偏移」的默认时刻）；与 {@link #curveRemindTime} 配合使用 */
    private LocalDateTime firstReminderAt;
    /**
     * 遗忘曲线除第一次外，各次提醒的统一时分（如 14:30）；不传则使用配置 {@code review.forgetting-curve.remind-time}。
     */
    private LocalTime curveRemindTime;
    private List<LocalDateTime> remindTimes;
}
