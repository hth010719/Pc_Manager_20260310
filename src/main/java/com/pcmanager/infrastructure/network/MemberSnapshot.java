package com.pcmanager.infrastructure.network;

public record MemberSnapshot(Long memberId, String loginId, String name, String phone, int remainingMinutes, int point, String grade) {
}
