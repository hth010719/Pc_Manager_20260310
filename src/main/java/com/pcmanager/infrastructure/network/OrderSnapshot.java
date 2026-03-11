package com.pcmanager.infrastructure.network;

/**
 * 서버에서 내려준 주문 목록 1건을 담는 읽기 전용 DTO다.
 *
 * 카운터/고객 화면은 이 스냅샷을 그대로 사용해 주문 번호, 좌석, 요약명, 상태, 총액을 표시한다.
 */
public record OrderSnapshot(Long orderId, Long seatId, String itemSummary, String status, int totalPrice) {
}
