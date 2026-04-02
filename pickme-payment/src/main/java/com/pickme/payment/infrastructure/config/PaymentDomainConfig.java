package com.pickme.payment.infrastructure.config;

import com.pickme.payment.domain.service.PaymentProcessingService;
import com.pickme.payment.infrastructure.external.PgPaymentGateway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentDomainConfig {

    @Bean
    public PaymentProcessingService paymentProcessingService(PgPaymentGateway pgPaymentGateway) {
        return new PaymentProcessingService(pgPaymentGateway);
    }
}
