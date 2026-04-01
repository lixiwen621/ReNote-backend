package com.renote.backend.mapper;

import com.renote.backend.entity.ReminderSchedule;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ReminderScheduleMapper {

    int insert(ReminderSchedule schedule);

    List<ReminderSchedule> findByTaskId(@Param("taskId") Long taskId);

    ReminderSchedule findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    List<ReminderSchedule> findDuePending(@Param("now") LocalDateTime now, @Param("limit") int limit);

    List<ReminderSchedule> findPendingInRangeByUser(@Param("userId") Long userId,
                                                       @Param("start") LocalDateTime start,
                                                       @Param("end") LocalDateTime end);

    List<ReminderSchedule> findPendingOnDateByUser(@Param("userId") Long userId, @Param("date") LocalDate date);

    /**
     * 当日排期中尚未产生对应 review_record 的条目（含已发通知的 sent），不含已取消。
     * 用于今日任务列表与「待复习」统计：仅用户完成复习后才排除。
     */
    List<ReminderSchedule> findIncompleteReviewOnDateByUser(@Param("userId") Long userId, @Param("date") LocalDate date);

    /**
     * 周视图：自然日落在 [startDate, endDate] 内的排期（不含已取消）
     */
    List<ReminderSchedule> findSchedulesInDateRangeByUser(@Param("userId") Long userId,
                                                          @Param("startDate") LocalDate startDate,
                                                          @Param("endDate") LocalDate endDate);

    ReminderSchedule findNextPendingByUser(@Param("userId") Long userId);

    int markSendingIfPending(@Param("id") Long id);

    int markSent(@Param("id") Long id);

    /** 用户完成复习：仅当仍为 pending 时标记为 sent，避免覆盖 sending/failed 等状态 */
    int markSentIfPending(@Param("id") Long id);

    int markFailure(@Param("id") Long id, @Param("status") Integer status, @Param("failReason") String failReason);

    int cancelByTaskId(@Param("taskId") Long taskId);

    int updateScheduledAtByIdAndUserId(@Param("id") Long id,
                                       @Param("userId") Long userId,
                                       @Param("scheduledAt") LocalDateTime scheduledAt);
}
