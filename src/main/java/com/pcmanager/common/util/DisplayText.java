package com.pcmanager.common.util;

/**
 * 서버 내부 코드값을 화면 표시용 한글 문구로 바꾸는 유틸리티다.
 *
 * UI에서는 enum/상수값을 직접 노출하지 않고 이 클래스만 통해 변환한다.
 * 덕분에 문구 정책이 바뀌어도 화면 곳곳을 직접 수정하지 않아도 된다.
 */
public final class DisplayText {
    private DisplayText() {
    }

    /**
     * 좌석 상태 코드를 카운터/고객 화면용 한글 텍스트로 변환한다.
     */
    public static String seatStatus(String value) {
        return switch (value) {
            case "AVAILABLE" -> "빈좌석";
            case "IN_USE" -> "사용 중";
            case "CLEANING" -> "청소 중";
            case "MAINTENANCE" -> "점검 중";
            default -> value;
        };
    }

    /**
     * 주문 상태 코드를 표시용 텍스트로 변환한다.
     */
    public static String orderStatus(String value) {
        return switch (value) {
            case "REQUESTED" -> "주문 들어옴";
            case "ACCEPTED" -> "주문 확인";
            case "PREPARING" -> "준비 중";
            case "DELIVERING" -> "전달 중";
            case "COMPLETED" -> "전달 완료";
            case "CANCELED" -> "취소";
            default -> value;
        };
    }

    /**
     * 고객 메시지 타입 코드를 한글 라벨로 변환한다.
     */
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

    /**
     * 메시지 발신 주체를 고객/카운터 문구로 바꾼다.
     */
    public static String senderType(String value) {
        return switch (value) {
            case "CUSTOMER" -> "고객";
            case "COUNTER" -> "카운터";
            default -> value;
        };
    }
}
