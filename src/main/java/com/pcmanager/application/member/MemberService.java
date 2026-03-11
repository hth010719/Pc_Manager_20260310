package com.pcmanager.application.member;

import com.pcmanager.common.exception.BusinessException;
import com.pcmanager.domain.member.Member;
import com.pcmanager.domain.member.UserRole;
import com.pcmanager.domain.seat.Seat;
import com.pcmanager.infrastructure.persistence.file.MemberFileStore;
import com.pcmanager.infrastructure.persistence.memory.MemoryStore;

import java.util.List;

public class MemberService {
    private final MemoryStore store;
    private final MemberFileStore memberFileStore;

    public MemberService(MemoryStore store, MemberFileStore memberFileStore) {
        this.store = store;
        this.memberFileStore = memberFileStore;
    }

    public List<Member> getAllMembers() {
        return store.getMembers();
    }

    public Member getById(Long memberId) {
        return store.getMembers().stream()
                .filter(member -> member.getMemberId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("존재하지 않는 회원입니다. memberId=" + memberId));
    }

    public Member getByLoginId(String loginId) {
        return store.getMembers().stream()
                .filter(member -> member.getLoginId().equals(loginId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("존재하지 않는 ID입니다. loginId=" + loginId));
    }

    public synchronized Member register(String loginId) {
        String normalizedLoginId = loginId == null ? "" : loginId.trim();
        if (normalizedLoginId.isEmpty()) {
            throw new BusinessException("ID를 입력해 주세요.");
        }
        boolean exists = store.getMembers().stream()
                .anyMatch(member -> member.getLoginId().equals(normalizedLoginId));
        if (exists) {
            throw new BusinessException("이미 사용 중인 ID입니다.");
        }
        Member member = new Member(
                store.nextMemberId(),
                normalizedLoginId,
                "",
                normalizedLoginId,
                "",
                0,
                0,
                "BRONZE",
                UserRole.CUSTOMER
        );
        store.getMembers().add(member);
        memberFileStore.saveMembers(store.getMembers());
        return member;
    }

    public synchronized void updateRemainingMinutes(Long memberId, int remainingMinutes) {
        Member member = getById(memberId);
        member.setRemainingMinutes(remainingMinutes);
        memberFileStore.saveMembers(store.getMembers());
    }

    public synchronized void addRemainingMinutes(String loginId, int minutes) {
        Member member = getByLoginId(loginId);
        member.addRemainingMinutes(minutes);
        memberFileStore.saveMembers(store.getMembers());
    }

    public synchronized void deleteMember(Long memberId) {
        Member member = getById(memberId);
        Seat occupiedSeat = store.getSeats().stream()
                .filter(seat -> memberId.equals(seat.getCurrentUserId()))
                .findFirst()
                .orElse(null);
        if (occupiedSeat != null) {
            throw new BusinessException("현재 좌석을 사용 중인 회원은 탈퇴시킬 수 없습니다. 좌석 " + occupiedSeat.getSeatNumber());
        }

        boolean removed = store.getMembers().remove(member);
        if (!removed) {
            throw new BusinessException("회원 탈퇴 처리에 실패했습니다.");
        }
        memberFileStore.saveMembers(store.getMembers());
    }
}
