package br.com.fiap.mercadopayment.domain;

import java.util.UUID;

public record PaymentResultEnvelope(
        UUID paymentAttemptId,
        PaymentResult payload) {
}
