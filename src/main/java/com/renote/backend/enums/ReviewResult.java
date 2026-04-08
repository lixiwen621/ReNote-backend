package com.renote.backend.enums;

import com.renote.backend.common.I18nPreconditions;

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
        I18nPreconditions.checkNotNull(code, "error.enum.reviewResult.required");
        ReviewResult found = null;
        for (ReviewResult result : values()) {
            if (result.code == code) {
                found = result;
                break;
            }
        }
        I18nPreconditions.checkArgument(found != null, "error.enum.reviewResult.invalid", code);
        return found;
    }
}
