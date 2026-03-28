package com.renote.backend.mapper;

import com.renote.backend.entity.NotifyChannelBinding;
import org.apache.ibatis.annotations.Param;

public interface NotifyChannelBindingMapper {

    NotifyChannelBinding findActiveByUserAndChannel(@Param("userId") Long userId, @Param("channel") Integer channel);

    int upsertBinding(@Param("userId") Long userId,
                      @Param("channel") Integer channel,
                      @Param("channelUserId") String channelUserId,
                      @Param("status") Integer status);
}

