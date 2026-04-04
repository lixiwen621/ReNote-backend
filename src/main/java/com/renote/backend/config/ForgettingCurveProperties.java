package com.renote.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

/**
 * 遗忘曲线自动排期：相对「创建当日」（用户时区）的天数偏移与固定提醒时刻。
 * <p>
 * 配置项见 {@code review.forgetting-curve.*}
 */
@Data
@ConfigurationProperties(prefix = "review.forgetting-curve")
public class ForgettingCurveProperties {

    private static final List<Integer> DEFAULT_DAY_OFFSETS = List.of(1, 2, 4, 7, 15, 20);

    /**
     * 相对创建当日的天数偏移，例如 1 表示次日。
     */
    private List<Integer> dayOffsets = DEFAULT_DAY_OFFSETS;

    /**
     * 每日固定提醒时刻（与任务 {@code timezone} 的 wall-clock 语义一致）。
     */
    private LocalTime remindTime = LocalTime.of(14, 30);

    /**
     * 过滤非法值、去重、升序；若配置为空或全部被过滤，则使用默认列表。
     */
    public List<Integer> getEffectiveDayOffsets() {
        if (dayOffsets == null || dayOffsets.isEmpty()) {
            return DEFAULT_DAY_OFFSETS;
        }
        List<Integer> resolved = dayOffsets.stream()
                .filter(Objects::nonNull)
                .filter(d -> d > 0)
                .distinct()
                .sorted()
                .toList();
        return resolved.isEmpty() ? DEFAULT_DAY_OFFSETS : resolved;
    }

    public LocalTime getEffectiveRemindTime() {
        return remindTime != null ? remindTime : LocalTime.of(14, 30);
    }
}
