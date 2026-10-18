package br.com.fiap.mercadopayment.service;

public class PaymentAttemptConflictException extends RuntimeException {

    public PaymentAttemptConflictException() {
        super("paymentAttemptId reutilizado com um payload diferente.");
    }
}
