package com.pickme.member.infrastructure.persistence;

import com.pickme.member.domain.model.Email;
import com.pickme.member.domain.model.Member;
import com.pickme.member.domain.model.MemberId;
import com.pickme.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepository {

    private final JpaMemberRepository jpaRepository;

    @Override
    public Member save(Member member) {
        Optional<MemberJpaEntity> existing = jpaRepository.findById(member.getMemberId().getValue());
        if (existing.isPresent()) {
            MemberJpaEntity entity = existing.get();
            entity.update(
                    member.getName().getValue(),
                    member.getPhone().getValue(),
                    member.getPassword().getHashedValue(),
                    MemberJpaEntity.GradeJpa.valueOf(member.getGrade().name()),
                    MemberJpaEntity.StatusJpa.valueOf(member.getStatus().name()),
                    member.getAccumulatedPurchaseAmount()
            );
            return MemberMapper.toDomain(jpaRepository.save(entity));
        }
        return MemberMapper.toDomain(jpaRepository.save(MemberMapper.toJpaEntity(member)));
    }

    @Override
    public Optional<Member> findById(MemberId memberId) {
        return jpaRepository.findById(memberId.getValue()).map(MemberMapper::toDomain);
    }

    @Override
    public Optional<Member> findByEmail(Email email) {
        return jpaRepository.findByEmail(email.getValue()).map(MemberMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpaRepository.existsByEmail(email.getValue());
    }
}
