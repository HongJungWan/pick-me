package com.pickme.member.infrastructure.persistence;

import com.pickme.member.domain.model.Email;
import com.pickme.member.domain.model.Member;
import com.pickme.member.domain.model.MemberGrade;
import com.pickme.member.domain.model.MemberId;
import com.pickme.member.domain.model.MemberName;
import com.pickme.member.domain.model.MemberStatus;
import com.pickme.member.domain.model.Password;
import com.pickme.member.domain.model.PhoneNumber;

public final class MemberMapper {

    private MemberMapper() {}

    public static MemberJpaEntity toJpaEntity(Member member) {
        return new MemberJpaEntity(
                member.getMemberId().getValue(),
                member.getEmail().getValue(),
                member.getPassword().getHashedValue(),
                member.getName().getValue(),
                member.getPhone().getValue(),
                MemberJpaEntity.GradeJpa.valueOf(member.getGrade().name()),
                MemberJpaEntity.StatusJpa.valueOf(member.getStatus().name()),
                member.getAccumulatedPurchaseAmount(),
                member.getRegisteredAt()
        );
    }

    public static Member toDomain(MemberJpaEntity entity) {
        return Member.reconstitute(
                MemberId.of(entity.getId()),
                new Email(entity.getEmail()),
                Password.ofHashed(entity.getPassword()),
                new MemberName(entity.getName()),
                new PhoneNumber(entity.getPhone()),
                MemberGrade.valueOf(entity.getGrade().name()),
                MemberStatus.valueOf(entity.getStatus().name()),
                entity.getAccumulatedPurchaseAmount(),
                entity.getRegisteredAt()
        );
    }
}
