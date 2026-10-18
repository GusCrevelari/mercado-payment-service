package br.com.fiap.mercadopayment.service;

import br.com.fiap.mercadopayment.domain.PaymentResult;

public record ProcessedPayment(PaymentResult result, boolean duplicate) {
}
