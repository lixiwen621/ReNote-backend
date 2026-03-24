package com.renote.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewTask {
    private Long id;
    private Long userId;
    private String title;
    private String sourceType;
    private String noteUrl;
    private String noteContent;
    private String timezone;
    private String scheduleMode;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
