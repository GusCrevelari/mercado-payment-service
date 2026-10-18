package br.com.fiap.mercadopayment.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PaymentRequestEnvelope(
        @NotNull UUID paymentAttemptId,
        @NotNull @Valid PaymentRequest payload) {
}
