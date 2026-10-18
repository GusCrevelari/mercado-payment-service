package br.com.fiap.mercadopayment.service;

import br.com.fiap.mercadopayment.bank.BancoTranquiloClient;
import br.com.fiap.mercadopayment.bank.BankPaymentRequest;
import br.com.fiap.mercadopayment.bank.BankPaymentResponse;
import br.com.fiap.mercadopayment.domain.PaymentRequest;
import br.com.fiap.mercadopayment.domain.PaymentRequestEnvelope;
import br.com.fiap.mercadopayment.domain.PaymentResult;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Service;

@Service
public class PaymentProcessor {

    private final BancoTranquiloClient bancoTranquiloClient;
    private final ConcurrentMap<java.util.UUID, CachedAttempt> attempts = new ConcurrentHashMap<>();

    public PaymentProcessor(BancoTranquiloClient bancoTranquiloClient) {
        this.bancoTranquiloClient = bancoTranquiloClient;
    }

    public ProcessedPayment process(PaymentRequestEnvelope envelope) {
        AtomicBoolean duplicate = new AtomicBoolean(false);
        CachedAttempt cached = attempts.compute(envelope.paymentAttemptId(), (attemptId, current) -> {
            if (current != null) {
                duplicate.set(true);
                if (!current.request().equals(envelope.payload())) {
                    throw new PaymentAttemptConflictException();
                }
                return current;
            }
            return authorize(envelope);
        });
        return new ProcessedPayment(cached.result(), duplicate.get());
    }

    private CachedAttempt authorize(PaymentRequestEnvelope envelope) {
        PaymentRequest request = envelope.payload();
        BankPaymentResponse response = bancoTranquiloClient.authorize(new BankPaymentRequest(
                envelope.paymentAttemptId(),
                request.purchaseId(),
                request.correlationId(),
                request.amount(),
                request.currency()));
        validateBankResponse(envelope, response);
        PaymentResult result = new PaymentResult(
                request.purchaseId(),
                request.correlationId(),
                request.amount(),
                request.currency(),
                response.status());
        return new CachedAttempt(request, result);
    }

    private void validateBankResponse(PaymentRequestEnvelope envelope, BankPaymentResponse response) {
        PaymentRequest request = envelope.payload();
        if (response == null
                || response.paymentAttemptId() == null
                || !response.paymentAttemptId().equals(envelope.paymentAttemptId())
                || !request.purchaseId().equals(response.purchaseId())
                || !request.correlationId().equals(response.correlationId())
                || request.amount().compareTo(response.amount()) != 0
                || !request.currency().equals(response.currency())
                || response.status() == null) {
            throw new IllegalStateException("Resposta do Banco Tranquilo nao corresponde a requisicao.");
        }
    }

    private record CachedAttempt(PaymentRequest request, PaymentResult result) {
    }
}
