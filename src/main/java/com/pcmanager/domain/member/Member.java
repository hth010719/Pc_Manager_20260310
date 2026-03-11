package com.pcmanager.domain.member;

public class Member {
    private final Long memberId;
    private final String loginId;
    private final String password;
    private final String name;
    private final String phone;
    private int remainingMinutes;
    private int point;
    private final String grade;
    private final UserRole userRole;
    private boolean firstChargeBonusAvailable;

    public Member(Long memberId, String loginId, String password, String name, String phone,
                  int remainingMinutes, int point, String grade, UserRole userRole, boolean firstChargeBonusAvailable) {
        this.memberId = memberId;
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.remainingMinutes = remainingMinutes;
        this.point = point;
        this.grade = grade;
        this.userRole = userRole;
        this.firstChargeBonusAvailable = firstChargeBonusAvailable;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getLoginId() {
        return loginId;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public int getRemainingMinutes() {
        return remainingMinutes;
    }

    public int getPoint() {
        return point;
    }

    public String getGrade() {
        return grade;
    }

    public UserRole getUserRole() {
        return userRole;
    }

    public boolean isFirstChargeBonusAvailable() {
        return firstChargeBonusAvailable;
    }

    public void addRemainingMinutes(int minutes) {
        remainingMinutes += minutes;
    }

    public void setRemainingMinutes(int remainingMinutes) {
        this.remainingMinutes = Math.max(remainingMinutes, 0);
    }

    public int applyChargeWithFirstBonus(int minutes, int bonusMinutes) {
        int appliedMinutes = minutes;
        if (firstChargeBonusAvailable) {
            appliedMinutes += bonusMinutes;
            firstChargeBonusAvailable = false;
        }
        addRemainingMinutes(appliedMinutes);
        return appliedMinutes;
    }
}
