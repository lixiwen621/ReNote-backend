package com.renote.backend.enums;

public enum UserStatus {
    ACTIVE(1),
    DISABLED(2);

    private final int code;

    UserStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
