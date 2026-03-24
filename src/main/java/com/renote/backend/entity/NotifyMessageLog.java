package com.renote.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotifyMessageLog {
    private Long id;
    private Long scheduleId;
    private Long taskId;
    private Long userId;
    private Integer channel;
    private String requestId;
    private String messageTitle;
    private String messageBody;
    private Integer sendStatus;
    private String errorCode;
    private String errorMessage;
    private LocalDateTime sentAt;
}
