package com.pickme.member.api.request;

import jakarta.validation.constraints.NotBlank;

public record SignupRequest(
        @NotBlank String email,
        @NotBlank String password,
        @NotBlank String name,
        @NotBlank String phone
) {}
