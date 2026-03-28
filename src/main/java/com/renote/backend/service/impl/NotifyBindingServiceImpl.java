package com.renote.backend.service.impl;

import com.renote.backend.dto.WeChatBindingResponse;
import com.renote.backend.enums.NotifyChannel;
import com.renote.backend.mapper.NotifyChannelBindingMapper;
import com.renote.backend.service.NotifyBindingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotifyBindingServiceImpl implements NotifyBindingService {

    private static final int ACTIVE_STATUS = 1;

    private final NotifyChannelBindingMapper notifyChannelBindingMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WeChatBindingResponse bindWeChat(Long userId, String channelUserId) {
        notifyChannelBindingMapper.upsertBinding(
                userId,
                NotifyChannel.WECHAT.code(),
                channelUserId,
                ACTIVE_STATUS
        );
        return WeChatBindingResponse.builder()
                .userId(userId)
                .channel(NotifyChannel.WECHAT.code())
                .channelUserId(channelUserId)
                .status(ACTIVE_STATUS)
                .build();
    }
}

