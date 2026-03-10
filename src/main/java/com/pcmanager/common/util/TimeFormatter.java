package com.pcmanager.common.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class TimeFormatter {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private TimeFormatter() {
    }

    public static String formatRemaining(Duration duration) {
        long seconds = Math.max(duration.getSeconds(), 0L);
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainSeconds = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, remainSeconds);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : DATE_TIME_FORMATTER.format(dateTime);
    }
}
