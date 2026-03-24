package com.renote.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewTaskResponse {
    private Long id;
    private Long userId;
    private String title;
    private String sourceType;
    private String noteUrl;
    private String noteContent;
    private String timezone;
    private String scheduleMode;
    private String status;
}
