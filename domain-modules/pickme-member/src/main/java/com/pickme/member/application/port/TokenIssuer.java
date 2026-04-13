package com.pickme.member.application.port;

import java.util.UUID;

/**
 * 인증 토큰 발급 포트.
 *
 * <p>구체 구현(JWT, OAuth, Paseto 등)은 infrastructure 계층의 어댑터에서 제공한다.
 * application 계층은 본 인터페이스만 의존한다.</p>
 *
 * @see com.pickme.member.infrastructure.config.JwtProvider 현 구현체 (JWT 기반)
 */
public interface TokenIssuer {

    /** 액세스 토큰 발급 (단기 유효). */
    String createAccessToken(UUID memberId, String email);

    /** 리프레시 토큰 발급 (장기 유효). */
    String createRefreshToken(UUID memberId, String email);
}
