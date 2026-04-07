package com.renote.backend.mapper;

import com.renote.backend.entity.ReviewTaskAttachment;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ReviewTaskAttachmentMapper {

    int insert(ReviewTaskAttachment attachment);

    List<ReviewTaskAttachment> findByTaskIdAndUserId(@Param("taskId") Long taskId, @Param("userId") Long userId);
}
