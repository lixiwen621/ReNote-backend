package com.renote.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ManualWeChatNotifyResponse {
    private Boolean success;
    private String requestId;
    private String errorCode;
    private String errorMessage;
}

