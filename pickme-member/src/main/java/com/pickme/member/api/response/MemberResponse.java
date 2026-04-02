package com.pickme.member.api.response;

import com.pickme.member.domain.model.Member;

import java.time.Instant;
import java.util.UUID;

public record MemberResponse(
        UUID memberId,
        String email,
        String name,
        String phone,
        String grade,
        String status,
        Instant registeredAt
) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getMemberId().getValue(),
                member.getEmail().getValue(),
                member.getName().getValue(),
                member.getPhone().getValue(),
                member.getGrade().name(),
                member.getStatus().name(),
                member.getRegisteredAt()
        );
    }
}
