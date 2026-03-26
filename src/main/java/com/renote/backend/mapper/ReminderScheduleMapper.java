package com.renote.backend.mapper;

import com.renote.backend.entity.ReminderSchedule;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ReminderScheduleMapper {

    int insert(ReminderSchedule schedule);

    List<ReminderSchedule> findByTaskId(@Param("taskId") Long taskId);

    List<ReminderSchedule> findDuePending(@Param("now") LocalDateTime now, @Param("limit") int limit);

    List<ReminderSchedule> findPendingInRangeByUser(@Param("userId") Long userId,
                                                       @Param("start") LocalDateTime start,
                                                       @Param("end") LocalDateTime end);

    List<ReminderSchedule> findPendingOnDateByUser(@Param("userId") Long userId, @Param("date") LocalDate date);

    ReminderSchedule findNextPendingByUser(@Param("userId") Long userId);

    int markSendingIfPending(@Param("id") Long id);

    int markSent(@Param("id") Long id);

    int markFailure(@Param("id") Long id, @Param("status") Integer status, @Param("failReason") String failReason);

    int cancelByTaskId(@Param("taskId") Long taskId);
}
