package com.renote.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskAttachmentResponse {
    private Long id;
    private String originalFileName;
    private String contentType;
    private Long fileSize;
    /** 1=图片 2=文件 */
    private Integer fileType;
    private String fileUrl;
}
