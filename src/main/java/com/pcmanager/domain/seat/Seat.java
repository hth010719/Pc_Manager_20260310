package com.pcmanager.domain.seat;

import java.time.LocalDateTime;

/**
 * PC방 좌석의 현재 사용 상태를 표현하는 도메인 객체다.
 *
 * 좌석은 "상태 + 현재 사용자 + 로그인/종료 예정 시각"을 함께 들고 다닌다.
 * 서비스 계층은 이 객체의 메서드를 호출해서 입장, 퇴실, 시간 연장, 상태 전환을 처리한다.
 */
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

    /**
     * 이미 사용 중인 좌석의 종료 예정 시각만 뒤로 민다.
     * 비사용 좌석은 expectedEndTime이 없으므로 아무 일도 하지 않는다.
     */
    public void addMinutes(int minutes) {
        if (expectedEndTime != null) {
            expectedEndTime = expectedEndTime.plusMinutes(minutes);
        }
    }

    /**
     * 비회원/손님 입장 처리다.
     * 회원 ID는 비워두고 별칭만 표시한다.
     */
    public void enterGuest(String nickname, LocalDateTime loginAt, int initialMinutes) {
        this.currentUserId = null;
        this.currentNickname = nickname;
        this.loginTime = loginAt;
        this.expectedEndTime = loginAt.plusMinutes(initialMinutes);
        this.status = SeatStatus.IN_USE;
    }

    /**
     * 회원 입장 처리다.
     * 좌석에 회원 ID를 연결해 이후 종료 시 남은 시간을 회원 계정에 저장할 수 있게 한다.
     */
    public void enterMember(Long userId, String displayName, LocalDateTime loginAt, int initialMinutes) {
        this.currentUserId = userId;
        this.currentNickname = displayName;
        this.loginTime = loginAt;
        this.expectedEndTime = loginAt.plusMinutes(initialMinutes);
        this.status = SeatStatus.IN_USE;
    }

    /**
     * 정상 종료 시 좌석의 사용자 관련 정보를 모두 제거하고 빈좌석으로 되돌린다.
     */
    public void exit() {
        this.currentUserId = null;
        this.currentNickname = null;
        this.loginTime = null;
        this.expectedEndTime = null;
        this.status = SeatStatus.AVAILABLE;
    }

    /**
     * 좌석 상태를 강제로 바꾼다.
     * 사용 중이 아닌 상태로 바뀌면 기존 사용자 정보도 함께 제거해 불일치를 막는다.
     */
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
