package com.renote.backend.notify;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class WeChatNotifyClient {

    @Value("${notify.wechat.enabled:false}")
    private boolean enabled;

    @Value("${notify.wechat.mock-success:true}")
    private boolean mockSuccess;

    public NotifyResult sendReminder(Long userId, String title, String contentOrUrl) {
        if (!enabled) {
            return NotifyResult.success("MOCK_DISABLED_CHANNEL");
        }
        if (mockSuccess) {
            return NotifyResult.success("MOCK_WECHAT_REQUEST_ID");
        }
        if (!StringUtils.hasText(title)) {
            return NotifyResult.fail("INVALID_PARAM", "消息标题不能为空");
        }
        return NotifyResult.fail("NOT_IMPLEMENTED", "微信真实发送尚未接入，请配置企业微信/公众号API");
    }
}
