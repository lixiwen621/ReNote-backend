package com.renote.backend.controller;

import com.renote.backend.common.ApiResponse;
import com.renote.backend.dto.BindWeChatRequest;
import com.renote.backend.dto.ManualWeChatNotifyRequest;
import com.renote.backend.dto.ManualWeChatNotifyResponse;
import com.renote.backend.dto.WeChatBindingResponse;
import com.renote.backend.service.NotifyBindingService;
import com.renote.backend.service.NotifySendService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
public class NotifyBindingController {

    private final NotifyBindingService notifyBindingService;
    private final NotifySendService notifySendService;

    @PostMapping("/bind/wechat")
    public ApiResponse<WeChatBindingResponse> bindWeChat(
            Authentication authentication,
            @Valid @RequestBody BindWeChatRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(notifyBindingService.bindWeChat(userId, request.getChannelUserId()));
    }

    @PostMapping("/send/wechat")
    public ApiResponse<ManualWeChatNotifyResponse> sendWeChat(
            Authentication authentication,
            @Valid @RequestBody ManualWeChatNotifyRequest request) {
        Long userId = (Long) authentication.getPrincipal();
        return ApiResponse.success(notifySendService.sendWeChat(userId, request.getTitle(), request.getContent()));
    }
}

