package com.renote.backend.mapper;

import com.renote.backend.entity.ReviewTask;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface ReviewTaskMapper {

    int insert(ReviewTask reviewTask);

    ReviewTask findById(@Param("id") Long id);

    int updateLastReviewedAt(@Param("taskId") Long taskId, @Param("lastReviewedAt") LocalDateTime lastReviewedAt);

    int updateNextRemindAt(@Param("taskId") Long taskId, @Param("nextRemindAt") LocalDateTime nextRemindAt);
}
