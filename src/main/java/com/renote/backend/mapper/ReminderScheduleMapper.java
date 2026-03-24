package com.renote.backend.mapper;

import com.renote.backend.entity.ReminderSchedule;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReminderScheduleMapper {

    int insert(ReminderSchedule schedule);

    List<ReminderSchedule> findByTaskId(@Param("taskId") Long taskId);

    List<ReminderSchedule> findDuePending(@Param("now") LocalDateTime now, @Param("limit") int limit);

    int markSendingIfPending(@Param("id") Long id);

    int markSent(@Param("id") Long id);

    int markFailure(@Param("id") Long id, @Param("status") Integer status, @Param("failReason") String failReason);

    int cancelByTaskId(@Param("taskId") Long taskId);
}
