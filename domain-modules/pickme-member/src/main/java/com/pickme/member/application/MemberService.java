package com.pickme.member.application;

import com.pickme.member.domain.model.Member;
import com.pickme.member.domain.model.MemberId;
import com.pickme.member.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public Member getMember(UUID memberId) {
        return memberRepository.findById(MemberId.of(memberId))
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다: " + memberId));
    }
}
