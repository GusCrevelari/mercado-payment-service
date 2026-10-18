package br.com.fiap.mercadopayment.bank;

public interface BancoTranquiloClient {

    BankPaymentResponse authorize(BankPaymentRequest request);
}
