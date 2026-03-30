package com.renote.backend.notify;

import com.renote.backend.entity.NotifyChannelBinding;
import com.renote.backend.enums.NotifyChannel;
import com.renote.backend.mapper.NotifyChannelBindingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WeChatNotifyClient {

    private static final String TOKEN_URL = "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid={corpId}&corpsecret={secret}";
    private static final String SEND_URL = "https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token={accessToken}";

    private final NotifyChannelBindingMapper notifyChannelBindingMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${notify.wechat.enabled:false}")
    private boolean enabled;

    @Value("${notify.wechat.mock-success:true}")
    private boolean mockSuccess;

    @Value("${notify.wechat.corp-id:}")
    private String corpId;

    @Value("${notify.wechat.agent-id:0}")
    private Integer agentId;

    @Value("${notify.wechat.secret:}")
    private String secret;

    private volatile String cachedAccessToken;
    private volatile long tokenExpireAtMs;

    public NotifyResult sendReminder(Long userId, String title, String contentOrUrl) {
        if (!enabled) {
            return NotifyResult.success(uniqueRequestId("MOCK_DISABLED_CHANNEL"));
        }
        if (mockSuccess) {
            return NotifyResult.success(uniqueRequestId("MOCK_WECHAT_REQUEST_ID"));
        }
        if (!StringUtils.hasText(title)) {
            return NotifyResult.fail("INVALID_PARAM", "消息标题不能为空");
        }
        if (!StringUtils.hasText(corpId) || !StringUtils.hasText(secret) || agentId == null || agentId <= 0) {
            return NotifyResult.fail("WECHAT_CONFIG_MISSING", "企业微信配置不完整，请检查 corp-id/agent-id/secret");
        }

        NotifyChannelBinding binding = notifyChannelBindingMapper.findActiveByUserAndChannel(userId, NotifyChannel.WECHAT.code());
        if (binding == null || !StringUtils.hasText(binding.getChannelUserId())) {
            return NotifyResult.fail("WECHAT_BINDING_NOT_FOUND", "未找到用户微信绑定，请先绑定 channel_user_id(userid)");
        }

        try {
            String accessToken = getAccessToken();
            if (!StringUtils.hasText(accessToken)) {
                return NotifyResult.fail("WECHAT_TOKEN_EMPTY", "获取企业微信 access_token 失败");
            }

            Map<String, Object> body = new HashMap<>();
            body.put("touser", binding.getChannelUserId());
            body.put("msgtype", "text");
            body.put("agentid", agentId);
            body.put("safe", 0);
            Map<String, String> text = new HashMap<>();
            text.put("content", buildContent(title, contentOrUrl));
            body.put("text", text);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(
                    SEND_URL,
                    request,
                    Map.class,
                    Map.of("accessToken", accessToken));
            if (response == null) {
                return NotifyResult.fail("WECHAT_EMPTY_RESPONSE", "企业微信发送返回为空");
            }
            int errCode = toInt(response.get("errcode"), -1);
            String errMsg = String.valueOf(response.getOrDefault("errmsg", ""));
            if (errCode != 0) {
                return NotifyResult.fail("WECHAT_" + errCode, errMsg);
            }
            String msgId = String.valueOf(response.getOrDefault("msgid", ""));
            return NotifyResult.success(StringUtils.hasText(msgId) ? msgId : uniqueRequestId("WECHAT_NO_MSGID"));
        } catch (Exception ex) {
            return NotifyResult.fail("WECHAT_SEND_EXCEPTION", ex.getMessage());
        }
    }

    private String uniqueRequestId(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private String getAccessToken() {
        long now = System.currentTimeMillis();
        if (StringUtils.hasText(cachedAccessToken) && now < tokenExpireAtMs) {
            return cachedAccessToken;
        }
        synchronized (this) {
            now = System.currentTimeMillis();
            if (StringUtils.hasText(cachedAccessToken) && now < tokenExpireAtMs) {
                return cachedAccessToken;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.getForObject(
                    TOKEN_URL,
                    Map.class,
                    Map.of("corpId", corpId, "secret", secret));
            if (resp == null) {
                return null;
            }
            int errCode = toInt(resp.get("errcode"), -1);
            if (errCode != 0) {
                return null;
            }
            String token = String.valueOf(resp.getOrDefault("access_token", ""));
            int expiresIn = toInt(resp.get("expires_in"), 7200);
            if (!StringUtils.hasText(token)) {
                return null;
            }
            cachedAccessToken = token;
            tokenExpireAtMs = System.currentTimeMillis() + Math.max(1, expiresIn - 120) * 1000L;
            return cachedAccessToken;
        }
    }

    private String buildContent(String title, String contentOrUrl) {
        if (StringUtils.hasText(contentOrUrl)) {
            return "【ReNote复习提醒】\n" + title + "\n" + contentOrUrl;
        }
        return "【ReNote复习提醒】\n" + title;
    }

    private int toInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return defaultValue;
        }
    }
}
