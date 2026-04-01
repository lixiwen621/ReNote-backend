package com.renote.backend.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 周视图卡片：在 {@link TodayReviewTaskCardResponse} 基础上增加是否已完成复习。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WeekReviewTaskCardResponse extends TodayReviewTaskCardResponse {
    /** 该 schedule_id 是否已有对应 review_record */
    private Boolean reviewCompleted;
}
