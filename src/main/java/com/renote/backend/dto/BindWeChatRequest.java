package com.renote.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BindWeChatRequest {

    @NotBlank(message = "channelUserId不能为空")
    private String channelUserId;
}

