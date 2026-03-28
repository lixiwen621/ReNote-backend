package com.renote.backend.controller;

import com.renote.backend.notify.WeComCallbackCrypto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping({"/api/wechat/callback", "/api/wechat/ca"})
public class WeChatCallbackController {

    private static final Pattern ENCRYPT_PATTERN = Pattern.compile("<Encrypt><!\\[CDATA\\[(.+?)\\]\\]></Encrypt>");

    @Value("${notify.wechat.callback-token:}")
    private String callbackToken;

    @Value("${notify.wechat.callback-encoding-aes-key:}")
    private String callbackEncodingAesKey;

    @Value("${notify.wechat.corp-id:}")
    private String corpId;

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verify(
            @RequestParam(value = "msg_signature", required = false) String msgSignature,
            @RequestParam(value = "timestamp", required = false) String timestamp,
            @RequestParam(value = "nonce", required = false) String nonce,
            @RequestParam(value = "echostr", required = false) String echostr) {
        try {
            if (!isCallbackConfigured()) {
                log.warn("企业微信回调配置缺失: tokenSet={}, aesKeySet={}, corpIdSet={}",
                        StringUtils.hasText(callbackToken), StringUtils.hasText(callbackEncodingAesKey), StringUtils.hasText(corpId));
                return ResponseEntity.badRequest().body("callback config missing");
            }
            if (!StringUtils.hasText(msgSignature) || !StringUtils.hasText(timestamp)
                    || !StringUtils.hasText(nonce) || !StringUtils.hasText(echostr)) {
                log.warn("企业微信GET回调缺参: signature={}, timestamp={}, nonce={}, echostr={}",
                        StringUtils.hasText(msgSignature), StringUtils.hasText(timestamp),
                        StringUtils.hasText(nonce), StringUtils.hasText(echostr));
                return ResponseEntity.badRequest().body("missing required params");
            }
            boolean ok = WeComCallbackCrypto.verifySignature(callbackToken, timestamp, nonce, echostr, msgSignature);
            if (!ok) {
                log.warn("企业微信GET回调验签失败: timestamp={}, nonce={}, signaturePrefix={}",
                        timestamp, nonce, mask(msgSignature));
                return ResponseEntity.badRequest().body("invalid signature");
            }
            String plain = WeComCallbackCrypto.decrypt(callbackEncodingAesKey, echostr, corpId);
            return ResponseEntity.ok(plain);
        } catch (Exception ex) {
            log.error("企业微信回调URL校验失败: {}", ex.getMessage(), ex);
            return ResponseEntity.badRequest().body("verify failed");
        }
    }

    @PostMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> receive(
            @RequestParam(value = "msg_signature", required = false) String msgSignature,
            @RequestParam(value = "timestamp", required = false) String timestamp,
            @RequestParam(value = "nonce", required = false) String nonce,
            @RequestBody(required = false) String xmlBody) {
        try {
            if (!isCallbackConfigured()) {
                log.warn("企业微信回调配置缺失: tokenSet={}, aesKeySet={}, corpIdSet={}",
                        StringUtils.hasText(callbackToken), StringUtils.hasText(callbackEncodingAesKey), StringUtils.hasText(corpId));
                return ResponseEntity.badRequest().body("callback config missing");
            }
            if (!StringUtils.hasText(msgSignature) || !StringUtils.hasText(timestamp) || !StringUtils.hasText(nonce)) {
                log.warn("企业微信POST回调缺参: signature={}, timestamp={}, nonce={}",
                        StringUtils.hasText(msgSignature), StringUtils.hasText(timestamp), StringUtils.hasText(nonce));
                return ResponseEntity.badRequest().body("missing required params");
            }
            String encrypted = extractEncrypted(xmlBody);
            if (!StringUtils.hasText(encrypted)) {
                log.warn("企业微信POST回调缺少Encrypt字段");
                return ResponseEntity.badRequest().body("missing encrypt");
            }
            boolean ok = WeComCallbackCrypto.verifySignature(callbackToken, timestamp, nonce, encrypted, msgSignature);
            if (!ok) {
                log.warn("企业微信POST回调验签失败: timestamp={}, nonce={}, signaturePrefix={}",
                        timestamp, nonce, mask(msgSignature));
                return ResponseEntity.badRequest().body("invalid signature");
            }
            // 这里只做验签+解密验证，业务事件可按需继续解析 decryptedXml。
            String decryptedXml = WeComCallbackCrypto.decrypt(callbackEncodingAesKey, encrypted, corpId);
            log.info("收到企业微信回调事件，解密长度={}", decryptedXml.length());
            return ResponseEntity.ok("success");
        } catch (Exception ex) {
            log.error("企业微信回调处理失败: {}", ex.getMessage(), ex);
            return ResponseEntity.badRequest().body("callback failed");
        }
    }

    private boolean isCallbackConfigured() {
        return StringUtils.hasText(callbackToken) && StringUtils.hasText(callbackEncodingAesKey) && StringUtils.hasText(corpId);
    }

    private String extractEncrypted(String xmlBody) {
        if (!StringUtils.hasText(xmlBody)) {
            return null;
        }
        Matcher matcher = ENCRYPT_PATTERN.matcher(xmlBody);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String mask(String input) {
        if (!StringUtils.hasText(input)) {
            return "empty";
        }
        int keep = Math.min(8, input.length());
        return input.substring(0, keep) + "***";
    }
}

