package com.renote.backend.mapper;

import com.renote.backend.entity.ReviewTask;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ReviewTaskMapper {

    @Insert("INSERT INTO review_task " +
            "(user_id, title, source_type, note_url, note_content, timezone, schedule_mode, status, created_at, updated_at) " +
            "VALUES " +
            "(#{userId}, #{title}, #{sourceType}, #{noteUrl}, #{noteContent}, #{timezone}, #{scheduleMode}, #{status}, NOW(3), NOW(3))")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ReviewTask reviewTask);

    @Select("SELECT id, user_id AS userId, title, source_type AS sourceType, note_url AS noteUrl, " +
            "note_content AS noteContent, timezone, schedule_mode AS scheduleMode, status, " +
            "created_at AS createdAt, updated_at AS updatedAt " +
            "FROM review_task WHERE id = #{id}")
    ReviewTask findById(@Param("id") Long id);
}
