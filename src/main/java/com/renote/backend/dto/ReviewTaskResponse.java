package com.renote.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ReviewTaskResponse {
    private Long id;
    private Long userId;
    private String title;
    private Integer sourceType;
    private String noteUrl;
    private String noteContent;
    private String timezone;
    private Integer scheduleMode;
    /** 1=全部自定义 2=遗忘曲线 */
    private Integer reminderStrategy;
    private Integer status;
    private List<TaskAttachmentResponse> attachments;
}
