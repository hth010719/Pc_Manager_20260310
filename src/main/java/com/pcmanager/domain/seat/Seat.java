package com.pcmanager.domain.seat;

import java.time.LocalDateTime;

public class Seat {
    private final Long seatId;
    private final int seatNumber;
    private SeatStatus status;
    private Long currentUserId;
    private String currentNickname;
    private LocalDateTime loginTime;
    private LocalDateTime expectedEndTime;

    public Seat(Long seatId, int seatNumber, SeatStatus status, Long currentUserId, String currentNickname, LocalDateTime loginTime, LocalDateTime expectedEndTime) {
        this.seatId = seatId;
        this.seatNumber = seatNumber;
        this.status = status;
        this.currentUserId = currentUserId;
        this.currentNickname = currentNickname;
        this.loginTime = loginTime;
        this.expectedEndTime = expectedEndTime;
    }

    public Long getSeatId() {
        return seatId;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public Long getCurrentUserId() {
        return currentUserId;
    }

    public String getCurrentNickname() {
        return currentNickname;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public LocalDateTime getExpectedEndTime() {
        return expectedEndTime;
    }

    public void addMinutes(int minutes) {
        if (expectedEndTime != null) {
            expectedEndTime = expectedEndTime.plusMinutes(minutes);
        }
    }

    public void enterGuest(String nickname, LocalDateTime loginAt, int initialMinutes) {
        this.currentUserId = null;
        this.currentNickname = nickname;
        this.loginTime = loginAt;
        this.expectedEndTime = loginAt.plusMinutes(initialMinutes);
        this.status = SeatStatus.IN_USE;
    }

    public void enterMember(Long userId, String displayName, LocalDateTime loginAt, int initialMinutes) {
        this.currentUserId = userId;
        this.currentNickname = displayName;
        this.loginTime = loginAt;
        this.expectedEndTime = loginAt.plusMinutes(initialMinutes);
        this.status = SeatStatus.IN_USE;
    }

    public void exit() {
        this.currentUserId = null;
        this.currentNickname = null;
        this.loginTime = null;
        this.expectedEndTime = null;
        this.status = SeatStatus.AVAILABLE;
    }

    public void changeStatus(SeatStatus status) {
        this.status = status;
        if (status != SeatStatus.IN_USE) {
            this.currentUserId = null;
            this.currentNickname = null;
            this.loginTime = null;
            this.expectedEndTime = null;
        }
    }
}
