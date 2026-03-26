package com.renote.backend.mapper;

import com.renote.backend.entity.User;
import org.apache.ibatis.annotations.Param;

public interface UserMapper {

    int insert(User user);

    User findByUsername(@Param("username") String username);

    User findById(@Param("id") Long id);
}
