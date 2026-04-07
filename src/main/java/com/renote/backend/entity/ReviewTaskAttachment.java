package com.renote.backend.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewTaskAttachment {
    private Long id;
    private Long taskId;
    private Long userId;
    private String originalFileName;
    private String storedFileName;
    private String contentType;
    private Long fileSize;
    /** 1=图片 2=文件 */
    private Integer fileType;
    private String storagePath;
    private String fileUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
