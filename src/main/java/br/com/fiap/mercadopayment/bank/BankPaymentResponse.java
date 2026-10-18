package br.com.fiap.mercadopayment.bank;

import br.com.fiap.mercadopayment.domain.PaymentStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record BankPaymentResponse(
        UUID paymentAttemptId,
        Long purchaseId,
        UUID correlationId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String authorizationCode,
        String reason) {
}
