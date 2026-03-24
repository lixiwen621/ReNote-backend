package com.renote.backend.enums;

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
        for (ScheduleMode mode : values()) {
            if (mode.code == code) {
                return mode;
            }
        }
        throw new IllegalArgumentException("scheduleMode非法: " + code);
    }
}
