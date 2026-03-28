package com.renote.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ManualWeChatNotifyRequest {

    @NotBlank(message = "title不能为空")
    private String title;

    private String content;
}

