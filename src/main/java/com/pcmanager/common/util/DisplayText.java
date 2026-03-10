package com.pcmanager.common.util;

public final class DisplayText {
    private DisplayText() {
    }

    public static String seatStatus(String value) {
        return switch (value) {
            case "AVAILABLE" -> "빈좌석";
            case "IN_USE" -> "사용 중";
            case "CLEANING" -> "청소 중";
            case "MAINTENANCE" -> "점검 중";
            default -> value;
        };
    }

    public static String orderStatus(String value) {
        return switch (value) {
            case "REQUESTED" -> "주문 들어옴";
            case "ACCEPTED" -> "주문 확인";
            case "PREPARING" -> "준비 중";
            case "DELIVERING" -> "전달 중";
            case "COMPLETED" -> "완료";
            case "CANCELED" -> "취소";
            default -> value;
        };
    }

    public static String messageType(String value) {
        return switch (value) {
            case "CALL_STAFF" -> "직원 호출";
            case "ASK_TIME" -> "남은 시간 문의";
            case "ORDER_INQUIRY" -> "주문 문의";
            case "TECH_SUPPORT" -> "자리 문제";
            case "GENERAL_CHAT" -> "일반 문의";
            default -> value;
        };
    }

    public static String senderType(String value) {
        return switch (value) {
            case "CUSTOMER" -> "고객";
            case "COUNTER" -> "카운터";
            default -> value;
        };
    }
}
