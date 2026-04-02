package com.pickme.member.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pickme.common.event.DomainEvent;
import com.pickme.common.outbox.OutboxEvent;
import com.pickme.common.outbox.OutboxRepository;
import com.pickme.member.api.request.LoginRequest;
import com.pickme.member.api.request.SignupRequest;
import com.pickme.member.api.response.TokenResponse;
import com.pickme.member.domain.model.Email;
import com.pickme.member.domain.model.Member;
import com.pickme.member.domain.model.MemberName;
import com.pickme.member.domain.model.Password;
import com.pickme.member.domain.model.PhoneNumber;
import com.pickme.member.domain.repository.MemberRepository;
import com.pickme.member.infrastructure.config.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public Member signup(SignupRequest request) {
        Email email = new Email(request.email());

        if (memberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다: " + request.email());
        }

        String hashedPassword = passwordEncoder.encode(request.password());
        Member member = Member.register(
                email,
                Password.ofHashed(hashedPassword),
                new MemberName(request.name()),
                new PhoneNumber(request.phone())
        );

        Member saved = memberRepository.save(member);
        publishDomainEvents(member);
        return saved;
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        Email email = new Email(request.email());
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다"));

        if (!passwordEncoder.matches(request.password(), member.getPassword().getHashedValue())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다");
        }

        String accessToken = jwtProvider.createAccessToken(member.getMemberId().getValue(), email.getValue());
        String refreshToken = jwtProvider.createRefreshToken(member.getMemberId().getValue(), email.getValue());

        return new TokenResponse(accessToken, refreshToken);
    }

    private void publishDomainEvents(Member member) {
        for (DomainEvent event : member.getDomainEvents()) {
            try {
                String payload = objectMapper.writeValueAsString(event);
                outboxRepository.save(OutboxEvent.from(event, payload));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("이벤트 직렬화 실패", e);
            }
        }
        member.clearDomainEvents();
    }
}
