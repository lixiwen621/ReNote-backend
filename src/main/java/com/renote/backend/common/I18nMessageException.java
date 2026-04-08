package com.renote.backend.common;

import lombok.Getter;

/**
 * 携带国际化消息键的业务异常，由 {@link com.renote.backend.interceptor.ControllerGlobalExceptionHandler} 解析为文案。
 *
 * @author tongguo.liu
 * @since 2026-04-08 14:00:00
 */
@Getter
public class I18nMessageException extends RuntimeException {

    private final String messageKey;
    private final Object[] args;

    private I18nMessageException(String messageKey, Throwable cause, Object[] args) {
        super(messageKey, cause);
        this.messageKey = messageKey;
        this.args = args != null ? args : new Object[0];
    }

    public static I18nMessageException of(String messageKey) {
        return new I18nMessageException(messageKey, null, new Object[0]);
    }

    public static I18nMessageException of(String messageKey, Object... args) {
        return new I18nMessageException(messageKey, null, args);
    }

    public static I18nMessageException of(String messageKey, Throwable cause) {
        return new I18nMessageException(messageKey, cause, new Object[0]);
    }

    public static I18nMessageException of(String messageKey, Throwable cause, Object... args) {
        return new I18nMessageException(messageKey, cause, args);
    }
}
