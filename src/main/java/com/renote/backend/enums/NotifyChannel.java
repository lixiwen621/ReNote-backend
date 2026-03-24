package com.renote.backend.enums;

public enum NotifyChannel {
    WECHAT(1, "wechat");

    private final int code;
    private final String text;

    NotifyChannel(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public int code() {
        return code;
    }
}
