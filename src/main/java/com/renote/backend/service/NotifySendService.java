package com.renote.backend.service;

import com.renote.backend.dto.ManualWeChatNotifyResponse;

public interface NotifySendService {

    ManualWeChatNotifyResponse sendWeChat(Long userId, String title, String content);
}

