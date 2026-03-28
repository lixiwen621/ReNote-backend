package com.renote.backend.service;

import com.renote.backend.dto.WeChatBindingResponse;

public interface NotifyBindingService {

    WeChatBindingResponse bindWeChat(Long userId, String channelUserId);
}

