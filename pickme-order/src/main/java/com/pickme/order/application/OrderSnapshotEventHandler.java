package com.pickme.order.application;

import com.pickme.common.idempotency.IdempotencyFilter;
import com.pickme.order.infrastructure.snapshot.MemberSnapshotEntity;
import com.pickme.order.infrastructure.snapshot.MemberSnapshotRepository;
import com.pickme.order.infrastructure.snapshot.ProductSnapshotEntity;
import com.pickme.order.infrastructure.snapshot.ProductSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSnapshotEventHandler {

    private final ProductSnapshotRepository productSnapshotRepository;
    private final MemberSnapshotRepository memberSnapshotRepository;
    private final IdempotencyFilter idempotencyFilter;

    @Transactional
    public void handleProductRegistered(UUID eventId, UUID productId, String productName, long sellingPrice) {
        if (idempotencyFilter.isDuplicate(eventId)) return;

        productSnapshotRepository.findById(productId).ifPresentOrElse(
                snapshot -> snapshot.update(productName, sellingPrice),
                () -> productSnapshotRepository.save(new ProductSnapshotEntity(productId, productName, sellingPrice))
        );

        idempotencyFilter.markProcessed(eventId, "ProductRegisteredEvent");
        log.info("Product 스냅샷 생성/갱신: productId={}", productId);
    }

    @Transactional
    public void handleProductInfoChanged(UUID eventId, UUID productId, String productName) {
        if (idempotencyFilter.isDuplicate(eventId)) return;

        productSnapshotRepository.findById(productId).ifPresent(
                snapshot -> snapshot.update(productName, snapshot.getSellingPrice())
        );

        idempotencyFilter.markProcessed(eventId, "ProductInfoChangedEvent");
        log.info("Product 스냅샷 갱신: productId={}", productId);
    }

    @Transactional
    public void handleProductPriceChanged(UUID eventId, UUID productId, long newPrice) {
        if (idempotencyFilter.isDuplicate(eventId)) return;

        productSnapshotRepository.findById(productId).ifPresent(
                snapshot -> snapshot.update(snapshot.getProductName(), newPrice)
        );

        idempotencyFilter.markProcessed(eventId, "ProductPriceChangedEvent");
        log.info("Product 스냅샷 가격 갱신: productId={}, newPrice={}", productId, newPrice);
    }

    @Transactional
    public void handleMemberRegistered(UUID eventId, UUID memberId, String name) {
        if (idempotencyFilter.isDuplicate(eventId)) return;

        memberSnapshotRepository.findById(memberId).ifPresentOrElse(
                snapshot -> snapshot.update(name, snapshot.getGrade()),
                () -> memberSnapshotRepository.save(new MemberSnapshotEntity(memberId, name, "NORMAL"))
        );

        idempotencyFilter.markProcessed(eventId, "MemberRegisteredEvent");
        log.info("Member 스냅샷 생성: memberId={}", memberId);
    }

    @Transactional
    public void handleMemberGradeChanged(UUID eventId, UUID memberId, String newGrade) {
        if (idempotencyFilter.isDuplicate(eventId)) return;

        memberSnapshotRepository.findById(memberId).ifPresent(
                snapshot -> snapshot.update(snapshot.getName(), newGrade)
        );

        idempotencyFilter.markProcessed(eventId, "MemberGradeChangedEvent");
        log.info("Member 스냅샷 등급 갱신: memberId={}, grade={}", memberId, newGrade);
    }
}
