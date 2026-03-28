package com.renote.backend.service.impl;

import com.renote.backend.dto.ManualWeChatNotifyResponse;
import com.renote.backend.notify.NotifyResult;
import com.renote.backend.notify.WeChatNotifyClient;
import com.renote.backend.service.NotifySendService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotifySendServiceImpl implements NotifySendService {

    private final WeChatNotifyClient weChatNotifyClient;

    @Override
    public ManualWeChatNotifyResponse sendWeChat(Long userId, String title, String content) {
        NotifyResult result = weChatNotifyClient.sendReminder(userId, title, content);
        return ManualWeChatNotifyResponse.builder()
                .success(result.isSuccess())
                .requestId(result.getRequestId())
                .errorCode(result.getErrorCode())
                .errorMessage(result.getErrorMessage())
                .build();
    }
}

