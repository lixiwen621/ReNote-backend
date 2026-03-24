package com.renote.backend.enums;

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
        if (code == null) {
            throw new IllegalArgumentException("sourceType不能为空");
        }
        for (NoteSourceType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("sourceType非法: " + code);
    }
}
