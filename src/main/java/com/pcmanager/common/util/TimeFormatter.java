package com.pcmanager.common.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 남은 시간과 시각을 화면 표시용 문자열로 바꾸는 유틸리티다.
 */
public final class TimeFormatter {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private TimeFormatter() {
    }

    /**
     * 남은 시간을 `HH:mm:ss` 형식으로 변환한다.
     * 이미 시간이 지난 경우에도 음수 대신 00:00:00부터 보여주도록 0으로 보정한다.
     */
    public static String formatRemaining(Duration duration) {
        long seconds = Math.max(duration.getSeconds(), 0L);
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainSeconds = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, remainSeconds);
    }

    /**
     * null 안전하게 날짜/시각을 렌더링한다.
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : DATE_TIME_FORMATTER.format(dateTime);
    }
}
