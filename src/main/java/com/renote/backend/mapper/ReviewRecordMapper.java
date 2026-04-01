package com.renote.backend.mapper;

import com.renote.backend.entity.ReviewRecord;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ReviewRecordMapper {

    int insert(ReviewRecord record);

    int countByUserIdAndScheduleId(@Param("userId") Long userId, @Param("scheduleId") Long scheduleId);

    int countReviewedInRangeByUser(@Param("userId") Long userId,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);

    int countReviewedOnDateByUser(@Param("userId") Long userId, @Param("date") LocalDate date);

    /** 在给定 schedule_id 集合中，已存在复习记录的 id 列表 */
    List<Long> findCompletedScheduleIdsByUserAndScheduleIds(@Param("userId") Long userId,
                                                          @Param("scheduleIds") List<Long> scheduleIds);
}
