package com.pickme.member.api;

import com.pickme.member.api.response.MemberResponse;
import com.pickme.member.application.MemberService;
import com.pickme.member.domain.model.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/{memberId}")
    public ResponseEntity<MemberResponse> getMember(@PathVariable UUID memberId) {
        Member member = memberService.getMember(memberId);
        return ResponseEntity.ok(MemberResponse.from(member));
    }
}
