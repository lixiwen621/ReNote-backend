package com.renote.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WeChatBindingResponse {
    private Long userId;
    private Integer channel;
    private String channelUserId;
    private Integer status;
}

