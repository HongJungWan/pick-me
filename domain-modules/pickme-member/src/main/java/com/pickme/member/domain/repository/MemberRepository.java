package com.pickme.member.domain.repository;

import com.pickme.member.domain.model.Email;
import com.pickme.member.domain.model.Member;
import com.pickme.member.domain.model.MemberId;

import java.util.Optional;

public interface MemberRepository {

    Member save(Member member);

    Optional<Member> findById(MemberId memberId);

    Optional<Member> findByEmail(Email email);

    boolean existsByEmail(Email email);
}
