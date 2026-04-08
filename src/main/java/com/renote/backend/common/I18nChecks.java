package com.renote.backend.common;

/**
 * @deprecated 请使用 {@link I18nPreconditions}（与 Guava {@code Preconditions} 命名一致）。
 */
@Deprecated(since = "0.0.1", forRemoval = false)
public final class I18nChecks {

    private I18nChecks() {
    }

    public static void checkState(boolean expression, String messageKey) {
        I18nPreconditions.checkState(expression, messageKey);
    }

    public static void checkState(boolean expression, String messageKey, Object... args) {
        I18nPreconditions.checkState(expression, messageKey, args);
    }
}
