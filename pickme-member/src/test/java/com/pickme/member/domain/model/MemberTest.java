package com.pickme.member.domain.model;

import com.pickme.member.domain.event.MemberGradeChangedEvent;
import com.pickme.member.domain.event.MemberRegisteredEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberTest {

    @Test
    void 회원가입_정상요청_MemberRegisteredEvent발행() {
        Member member = createMember();

        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(member.getGrade()).isEqualTo(MemberGrade.NORMAL);
        assertThat(member.getDomainEvents()).hasSize(1);
        assertThat(member.getDomainEvents().get(0)).isInstanceOf(MemberRegisteredEvent.class);
    }

    @Test
    void 이메일_잘못된형식_예외발생() {
        assertThatThrownBy(() -> new Email("invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않은 이메일");
    }

    @Test
    void 이메일_정상형식_소문자변환() {
        Email email = new Email("Test@Example.COM");
        assertThat(email.getValue()).isEqualTo("test@example.com");
    }

    @Test
    void 전화번호_잘못된형식_예외발생() {
        assertThatThrownBy(() -> new PhoneNumber("01012345678"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("010-XXXX-XXXX");
    }

    @Test
    void 전화번호_정상형식() {
        PhoneNumber phone = new PhoneNumber("010-1234-5678");
        assertThat(phone.getValue()).isEqualTo("010-1234-5678");
    }

    @Test
    void 이름_1자_예외발생() {
        assertThatThrownBy(() -> new MemberName("홍"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2~50자");
    }

    @Test
    void 등급재계산_NORMAL에서_SILVER() {
        Member member = createMember();
        member.clearDomainEvents();

        member.addPurchaseAmount(100_000);

        assertThat(member.getGrade()).isEqualTo(MemberGrade.SILVER);
        assertThat(member.getDomainEvents()).hasSize(1);
        assertThat(member.getDomainEvents().get(0)).isInstanceOf(MemberGradeChangedEvent.class);
    }

    @Test
    void 등급재계산_누적_VVIP() {
        Member member = createMember();
        member.addPurchaseAmount(10_000_000);

        assertThat(member.getGrade()).isEqualTo(MemberGrade.VVIP);
    }

    @Test
    void 등급재계산_변동없으면_이벤트미발행() {
        Member member = createMember();
        member.clearDomainEvents();

        member.addPurchaseAmount(50_000);

        assertThat(member.getGrade()).isEqualTo(MemberGrade.NORMAL);
        assertThat(member.getDomainEvents()).isEmpty();
    }

    @Test
    void 회원탈퇴_성공() {
        Member member = createMember();
        member.withdraw();
        assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
    }

    @Test
    void 회원탈퇴_이미탈퇴_예외발생() {
        Member member = createMember();
        member.withdraw();
        assertThatThrownBy(member::withdraw)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 탈퇴");
    }

    @Test
    void MemberGrade_fromAccumulatedAmount() {
        assertThat(MemberGrade.fromAccumulatedAmount(0)).isEqualTo(MemberGrade.NORMAL);
        assertThat(MemberGrade.fromAccumulatedAmount(99_999)).isEqualTo(MemberGrade.NORMAL);
        assertThat(MemberGrade.fromAccumulatedAmount(100_000)).isEqualTo(MemberGrade.SILVER);
        assertThat(MemberGrade.fromAccumulatedAmount(500_000)).isEqualTo(MemberGrade.GOLD);
        assertThat(MemberGrade.fromAccumulatedAmount(2_000_000)).isEqualTo(MemberGrade.VIP);
        assertThat(MemberGrade.fromAccumulatedAmount(10_000_000)).isEqualTo(MemberGrade.VVIP);
    }

    private Member createMember() {
        return Member.register(
                new Email("test@example.com"),
                Password.ofHashed("$2a$10$hashedpassword"),
                new MemberName("홍정완"),
                new PhoneNumber("010-1234-5678")
        );
    }
}
