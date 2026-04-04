package com.renote.backend.enums;

/**
 * 创建任务时的提醒时间策略（与 {@code schedule_mode} 配合落库）。
 */
public enum ReminderStrategy {
    /** 全部时间点由 {@code remindTimes} 指定 */
    FULL_CUSTOM(1),
    /** 遗忘曲线：第一次可单独指定 {@code firstReminderAt}，其余按天偏移 + 统一 {@code curveRemindTime} */
    FORGETTING_CURVE(2);

    private final int code;

    ReminderStrategy(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static ReminderStrategy fromCode(Integer code) {
        if (code == null) {
            throw new IllegalArgumentException("reminderStrategy不能为空");
        }
        for (ReminderStrategy s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        throw new IllegalArgumentException("reminderStrategy非法: " + code);
    }
}
