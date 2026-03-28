package com.renote.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotifyChannelBinding {
    private Long id;
    private Long userId;
    private Integer channel;
    private String channelUserId;
    private Integer status;
    private String extra;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

