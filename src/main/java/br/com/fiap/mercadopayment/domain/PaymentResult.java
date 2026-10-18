package br.com.fiap.mercadopayment.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResult(
        Long purchaseId,
        UUID correlationId,
        BigDecimal amount,
        String currency,
        PaymentStatus status) {
}
