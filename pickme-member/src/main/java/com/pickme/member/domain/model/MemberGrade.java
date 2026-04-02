package com.pickme.member.domain.model;

public enum MemberGrade {

    NORMAL(0),
    SILVER(100_000),
    GOLD(500_000),
    VIP(2_000_000),
    VVIP(10_000_000);

    private final long threshold;

    MemberGrade(long threshold) {
        this.threshold = threshold;
    }

    public static MemberGrade fromAccumulatedAmount(long amount) {
        MemberGrade result = NORMAL;
        for (MemberGrade grade : values()) {
            if (amount >= grade.threshold) {
                result = grade;
            }
        }
        return result;
    }

    public long getThreshold() {
        return threshold;
    }
}
