package com.renote.backend.enums;

import com.renote.backend.common.I18nPreconditions;

public enum NoteSourceType {
    EVERNOTE(1, "evernote"),
    OTHER(2, "other");

    private final int code;
    private final String text;

    NoteSourceType(int code, String text) {
        this.code = code;
        this.text = text;
    }

    public int code() {
        return code;
    }

    public static NoteSourceType fromCode(Integer code) {
        I18nPreconditions.checkNotNull(code, "error.enum.sourceType.required");
        NoteSourceType found = null;
        for (NoteSourceType type : values()) {
            if (type.code == code) {
                found = type;
                break;
            }
        }
        I18nPreconditions.checkArgument(found != null, "error.enum.sourceType.invalid", code);
        return found;
    }
}
