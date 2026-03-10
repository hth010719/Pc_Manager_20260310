package com.pcmanager.infrastructure.network;

public record OrderSnapshot(Long orderId, Long seatId, String itemSummary, String status, int totalPrice) {
}
