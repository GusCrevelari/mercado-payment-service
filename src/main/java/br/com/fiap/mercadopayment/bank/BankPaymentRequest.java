package br.com.fiap.mercadopayment.bank;

import java.math.BigDecimal;
import java.util.UUID;

public record BankPaymentRequest(
        UUID paymentAttemptId,
        Long purchaseId,
        UUID correlationId,
        BigDecimal amount,
        String currency) {
}
