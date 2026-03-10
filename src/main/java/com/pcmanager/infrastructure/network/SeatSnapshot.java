package com.pcmanager.infrastructure.network;

public record SeatSnapshot(Long seatId, int seatNumber, String status, String userName, String remainingTime) {
}
