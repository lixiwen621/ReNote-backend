package com.renote.backend.enums;

public enum ReviewResult {
    DONE(1, "done"),
    PARTIAL(2, "partial"),
    SKIP(3, "skip");

    private final int code;
    private final String text;

    ReviewResult(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public int code() {
        return code;
    }

    public static ReviewResult fromCode(Integer code) {
        if (code == null) {
            throw new IllegalArgumentException("reviewResult不能为空");
        }
        for (ReviewResult result : values()) {
            if (result.code == code) {
                return result;
            }
        }
        throw new IllegalArgumentException("reviewResult非法: " + code);
    }
}
