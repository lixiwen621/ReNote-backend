package com.renote.backend.enums;

public enum NotifySendStatus {
    SUCCESS(1),
    FAILED(2);

    private final int code;

    NotifySendStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
