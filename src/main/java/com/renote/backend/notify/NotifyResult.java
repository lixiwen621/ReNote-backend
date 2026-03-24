package com.renote.backend.notify;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NotifyResult {
    private boolean success;
    private String requestId;
    private String errorCode;
    private String errorMessage;

    public static NotifyResult success(String requestId) {
        return new NotifyResult(true, requestId, null, null);
    }

    public static NotifyResult fail(String errorCode, String errorMessage) {
        return new NotifyResult(false, null, errorCode, errorMessage);
    }
}
