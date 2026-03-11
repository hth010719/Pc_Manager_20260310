package com.pcmanager.application.member;

import com.pcmanager.common.exception.BusinessException;
import com.pcmanager.domain.member.Member;
import com.pcmanager.domain.member.UserRole;
import com.pcmanager.domain.seat.Seat;
import com.pcmanager.infrastructure.persistence.file.MemberFileStore;
import com.pcmanager.infrastructure.persistence.memory.MemoryStore;

import java.util.List;

/**
 * 회원 조회/등록/시간 저장/탈퇴를 담당하는 서비스다.
 *
 * 회원 정보는 메모리 컬렉션에서 읽고 쓰되,
 * 변경이 생기면 MemberFileStore에 즉시 저장해 다음 실행에도 유지되게 한다.
 */
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

    /**
     * memberId로 회원을 조회한다.
     * 없으면 즉시 예외를 던져 상위 계층이 실패 메시지를 사용자에게 보여주게 한다.
     */
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

    /**
     * 최소 정보만 사용하는 간단 회원가입이다.
     * 로그인 ID를 정규화한 뒤 중복을 검사하고, 기본 등급/역할로 새 회원을 만든다.
     */
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

    /**
     * 고객 종료 시 남은 시간을 회원 계정에 다시 저장한다.
     */
    public synchronized void addRemainingMinutes(String loginId, int minutes) {
        Member member = getByLoginId(loginId);
        member.addRemainingMinutes(minutes);
        memberFileStore.saveMembers(store.getMembers());
    }

    /**
     * 회원 탈퇴 처리다.
     * 현재 좌석 사용 중인 회원은 좌석/회원 상태 불일치를 막기 위해 탈퇴를 막는다.
     */
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
