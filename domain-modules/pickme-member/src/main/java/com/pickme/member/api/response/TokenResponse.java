package com.pickme.member.api.response;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {}
