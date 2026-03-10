package com.pcmanager.infrastructure.network;

public record MessageSnapshot(Long seatId, String senderType, String messageType, String content) {
}
