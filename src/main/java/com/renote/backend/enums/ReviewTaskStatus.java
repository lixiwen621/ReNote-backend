package com.renote.backend.enums;

public enum ReviewTaskStatus {
    ACTIVE(1, "active"),
    PAUSED(2, "paused"),
    ARCHIVED(3, "archived"),
    DELETED(4, "deleted");

    private final int code;
    private final String text;

    ReviewTaskStatus(int code, String text) {
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
        for (ReviewTaskStatus status : values()) {
            if (status.code == code) {
                return status.text;
            }
        }
        return null;
    }
}
