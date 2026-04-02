package com.pickme.member.domain.model;

import com.pickme.common.event.DomainEvent;
import com.pickme.member.domain.event.MemberGradeChangedEvent;
import com.pickme.member.domain.event.MemberRegisteredEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Member {

    private final MemberId memberId;
    private final Email email;
    private Password password;
    private MemberName name;
    private PhoneNumber phone;
    private MemberGrade grade;
    private MemberStatus status;
    private long accumulatedPurchaseAmount;
    private final Instant registeredAt;
    private final List<DomainEvent> domainEvents;

    private Member(MemberId memberId, Email email, Password password, MemberName name,
                   PhoneNumber phone, MemberGrade grade, MemberStatus status,
                   long accumulatedPurchaseAmount, Instant registeredAt) {
        this.memberId = memberId;
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.grade = grade;
        this.status = status;
        this.accumulatedPurchaseAmount = accumulatedPurchaseAmount;
        this.registeredAt = registeredAt;
        this.domainEvents = new ArrayList<>();
    }

    public static Member register(Email email, Password password, MemberName name, PhoneNumber phone) {
        Member member = new Member(
                MemberId.generate(), email, password, name, phone,
                MemberGrade.NORMAL, MemberStatus.ACTIVE, 0, Instant.now()
        );
        member.domainEvents.add(new MemberRegisteredEvent(
                member.memberId.getValue(), name.getValue(), email.getValue()));
        return member;
    }

    public static Member reconstitute(MemberId memberId, Email email, Password password,
                                      MemberName name, PhoneNumber phone, MemberGrade grade,
                                      MemberStatus status, long accumulatedPurchaseAmount,
                                      Instant registeredAt) {
        return new Member(memberId, email, password, name, phone, grade, status,
                accumulatedPurchaseAmount, registeredAt);
    }

    public void addPurchaseAmount(long amount) {
        if (amount <= 0) return;
        this.accumulatedPurchaseAmount += amount;
        recalculateGrade();
    }

    private void recalculateGrade() {
        MemberGrade newGrade = MemberGrade.fromAccumulatedAmount(this.accumulatedPurchaseAmount);
        if (newGrade != this.grade) {
            String oldGradeName = this.grade.name();
            this.grade = newGrade;
            this.domainEvents.add(new MemberGradeChangedEvent(
                    this.memberId.getValue(), oldGradeName, newGrade.name()));
        }
    }

    public void changeName(MemberName newName) {
        this.name = newName;
    }

    public void changePhone(PhoneNumber newPhone) {
        this.phone = newPhone;
    }

    public void changePassword(Password newPassword) {
        this.password = newPassword;
    }

    public void withdraw() {
        if (this.status == MemberStatus.WITHDRAWN) {
            throw new IllegalStateException("이미 탈퇴한 회원입니다");
        }
        this.status = MemberStatus.WITHDRAWN;
    }

    public MemberId getMemberId() { return memberId; }
    public Email getEmail() { return email; }
    public Password getPassword() { return password; }
    public MemberName getName() { return name; }
    public PhoneNumber getPhone() { return phone; }
    public MemberGrade getGrade() { return grade; }
    public MemberStatus getStatus() { return status; }
    public long getAccumulatedPurchaseAmount() { return accumulatedPurchaseAmount; }
    public Instant getRegisteredAt() { return registeredAt; }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }
}
