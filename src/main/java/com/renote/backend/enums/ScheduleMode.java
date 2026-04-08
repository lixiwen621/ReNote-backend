package com.renote.backend.enums;

import com.renote.backend.common.I18nPreconditions;

public enum ScheduleMode {
    MANUAL(1, "manual"),
    FORGETTING_CURVE(2, "forgetting_curve");

    private final int code;
    private final String text;

    ScheduleMode(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public int code() {
        return code;
    }

    public static ScheduleMode fromCode(Integer code) {
        if (code == null) {
            return FORGETTING_CURVE;
        }
        ScheduleMode found = null;
        for (ScheduleMode mode : values()) {
            if (mode.code == code) {
                found = mode;
                break;
            }
        }
        I18nPreconditions.checkArgument(found != null, "error.enum.scheduleMode.invalid", code);
        return found;
    }
}
