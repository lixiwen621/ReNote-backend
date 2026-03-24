package com.renote.backend.enums;

public enum ReminderScheduleStatus {
    PENDING(1, "pending"),
    SENDING(2, "sending"),
    SENT(3, "sent"),
    FAILED(4, "failed"),
    CANCELLED(5, "cancelled");

    private final int code;
    private final String text;

    ReminderScheduleStatus(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public int code() {
        return code;
    }

    public String text() {
        return text;
    }

    public static String textOf(Integer code) {
        if (code == null) {
            return null;
        }
        for (ReminderScheduleStatus status : values()) {
            if (status.code == code) {
                return status.text;
            }
        }
        return null;
    }
}
