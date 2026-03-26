package com.renote.backend.mapper;

import com.renote.backend.entity.ReviewRecord;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface ReviewRecordMapper {

    int insert(ReviewRecord record);

    int countReviewedInRangeByUser(@Param("userId") Long userId,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);

    int countReviewedOnDateByUser(@Param("userId") Long userId, @Param("date") LocalDate date);
}
