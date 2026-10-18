package br.com.fiap.mercadopayment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import br.com.fiap.mercadopayment.bank.BancoTranquiloClient;
import br.com.fiap.mercadopayment.bank.BankPaymentRequest;
import br.com.fiap.mercadopayment.bank.BankPaymentResponse;
import br.com.fiap.mercadopayment.domain.PaymentRequest;
import br.com.fiap.mercadopayment.domain.PaymentRequestEnvelope;
import br.com.fiap.mercadopayment.domain.PaymentStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentProcessorTest {

    @Mock
    private BancoTranquiloClient client;

    @Test
    void shouldReuseResultForTheSameAttemptWithoutCallingBankAgain() {
        UUID attemptId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        PaymentRequest payload = new PaymentRequest(11L, correlationId, new BigDecimal("39.90"), "BRL");
        PaymentRequestEnvelope envelope = new PaymentRequestEnvelope(attemptId, payload);
        BankPaymentRequest bankRequest = new BankPaymentRequest(
                attemptId, 11L, correlationId, new BigDecimal("39.90"), "BRL");
        when(client.authorize(bankRequest)).thenReturn(new BankPaymentResponse(
                attemptId, 11L, correlationId, new BigDecimal("39.90"), "BRL",
                PaymentStatus.APPROVED, "BT-123", null));
        PaymentProcessor processor = new PaymentProcessor(client);

        ProcessedPayment first = processor.process(envelope);
        ProcessedPayment duplicate = processor.process(envelope);

        assertThat(first.duplicate()).isFalse();
        assertThat(duplicate.duplicate()).isTrue();
        assertThat(duplicate.result()).isEqualTo(first.result());
        verify(client).authorize(bankRequest);
        verifyNoMoreInteractions(client);
    }

    @Test
    void shouldRejectSameAttemptWithDifferentPayload() {
        UUID attemptId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        PaymentRequest firstPayload = new PaymentRequest(11L, correlationId, new BigDecimal("39.90"), "BRL");
        BankPaymentRequest bankRequest = new BankPaymentRequest(
                attemptId, 11L, correlationId, new BigDecimal("39.90"), "BRL");
        when(client.authorize(bankRequest)).thenReturn(new BankPaymentResponse(
                attemptId, 11L, correlationId, new BigDecimal("39.90"), "BRL",
                PaymentStatus.APPROVED, "BT-123", null));
        PaymentProcessor processor = new PaymentProcessor(client);
        processor.process(new PaymentRequestEnvelope(attemptId, firstPayload));

        PaymentRequest conflicting = new PaymentRequest(11L, correlationId, new BigDecimal("40.00"), "BRL");

        assertThatThrownBy(() -> processor.process(new PaymentRequestEnvelope(attemptId, conflicting)))
                .isInstanceOf(PaymentAttemptConflictException.class);
    }
}
