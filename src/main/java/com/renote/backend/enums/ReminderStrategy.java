package com.renote.backend.enums;

import com.renote.backend.common.I18nPreconditions;

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
        I18nPreconditions.checkNotNull(code, "error.enum.reminderStrategy.required");
        ReminderStrategy found = null;
        for (ReminderStrategy s : values()) {
            if (s.code == code) {
                found = s;
                break;
            }
        }
        I18nPreconditions.checkArgument(found != null, "error.enum.reminderStrategy.invalid", code);
        return found;
    }
}
