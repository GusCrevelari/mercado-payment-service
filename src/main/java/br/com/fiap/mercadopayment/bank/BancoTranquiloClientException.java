package br.com.fiap.mercadopayment.bank;

public class BancoTranquiloClientException extends RuntimeException {

    public BancoTranquiloClientException(String message) {
        super(message);
    }

    public BancoTranquiloClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
