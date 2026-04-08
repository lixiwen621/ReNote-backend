package com.renote.backend.common;

import com.google.common.base.Preconditions;

/**
 * 与 Guava {@link Preconditions} 用法对齐的国际化预检：失败时抛出 {@link I18nMessageException}，
 * 由全局异常处理器结合 {@code messages*.properties} 解析文案。
 * <p>
 * 不需要国际化的场景可继续使用 {@link Preconditions#checkNotNull(Object)} 等（抛出 NPE/IAE/ISE）。
 *
 * @author tongguo.liu
 * @since 2026-04-08 15:00:00
 */
public final class I18nPreconditions {

    private I18nPreconditions() {
    }

    /**
     * 等价于 {@code Preconditions.checkNotNull(reference)}，但使用消息键与占位参数。
     */
    public static <T> T checkNotNull(T reference, String messageKey) {
        if (reference == null) {
            throw I18nMessageException.of(messageKey);
        }
        return reference;
    }

    public static <T> T checkNotNull(T reference, String messageKey, Object... args) {
        if (reference == null) {
            throw I18nMessageException.of(messageKey, args);
        }
        return reference;
    }

    public static void checkArgument(boolean expression, String messageKey) {
        if (!expression) {
            throw I18nMessageException.of(messageKey);
        }
    }

    public static void checkArgument(boolean expression, String messageKey, Object... args) {
        if (!expression) {
            throw I18nMessageException.of(messageKey, args);
        }
    }

    public static void checkState(boolean expression, String messageKey) {
        if (!expression) {
            throw I18nMessageException.of(messageKey);
        }
    }

    public static void checkState(boolean expression, String messageKey, Object... args) {
        if (!expression) {
            throw I18nMessageException.of(messageKey, args);
        }
    }
}
