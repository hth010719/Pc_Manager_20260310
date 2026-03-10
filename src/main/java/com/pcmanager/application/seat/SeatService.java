package com.pcmanager.application.seat;

import com.pcmanager.common.exception.BusinessException;
import com.pcmanager.domain.member.Member;
import com.pcmanager.domain.seat.Seat;
import com.pcmanager.domain.seat.SeatStatus;
import com.pcmanager.infrastructure.persistence.memory.MemoryStore;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class SeatService {
    private final MemoryStore store;

    public SeatService(MemoryStore store) {
        this.store = store;
    }

    public List<Seat> getAllSeats() {
        return store.getSeats();
    }

    public Seat getById(Long seatId) {
        return store.getSeats().stream()
                .filter(seat -> seat.getSeatId().equals(seatId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("존재하지 않는 좌석입니다. seatId=" + seatId));
    }

    public Duration getRemainingDuration(Long seatId) {
        Seat seat = getById(seatId);
        if (seat.getExpectedEndTime() == null) {
            return Duration.ZERO;
        }
        return Duration.between(LocalDateTime.now(), seat.getExpectedEndTime());
    }

    public void extendTime(Long seatId, int minutes) {
        getById(seatId).addMinutes(minutes);
    }

    public synchronized Seat enterGuest(String nickname, int initialMinutes) {
        return store.getSeats().stream()
                .filter(seat -> seat.getStatus() == com.pcmanager.domain.seat.SeatStatus.AVAILABLE)
                .findFirst()
                .map(seat -> {
                    seat.enterGuest(nickname, LocalDateTime.now(), initialMinutes);
                    return seat;
                })
                .orElseThrow(() -> new BusinessException("빈 좌석이 없습니다."));
    }

    public synchronized Seat enterMember(Member member) {
        if (member.getRemainingMinutes() <= 0) {
            throw new BusinessException("잔여 시간이 없습니다. 시간충전을 먼저 해주세요.");
        }
        Seat occupiedSeat = store.getSeats().stream()
                .filter(seat -> member.getMemberId().equals(seat.getCurrentUserId()))
                .findFirst()
                .orElse(null);
        if (occupiedSeat != null) {
            return occupiedSeat;
        }
        return store.getSeats().stream()
                .filter(seat -> seat.getStatus() == com.pcmanager.domain.seat.SeatStatus.AVAILABLE)
                .findFirst()
                .map(seat -> {
                    seat.enterMember(member.getMemberId(), member.getLoginId(), LocalDateTime.now(), member.getRemainingMinutes());
                    return seat;
                })
                .orElseThrow(() -> new BusinessException("빈 좌석이 없습니다."));
    }

    public void forceExit(Long seatId) {
        getById(seatId).exit();
    }

    public void changeSeatStatus(Long seatId, SeatStatus status) {
        Seat seat = getById(seatId);
        if (seat.getStatus() == SeatStatus.IN_USE && status != SeatStatus.AVAILABLE) {
            throw new BusinessException("사용 중인 좌석은 먼저 종료 후 상태를 바꿔 주세요.");
        }
        if (seat.getStatus() == SeatStatus.IN_USE && status == SeatStatus.AVAILABLE) {
            seat.exit();
            return;
        }
        seat.changeStatus(status);
    }
}
