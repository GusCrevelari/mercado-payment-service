package br.com.fiap.mercadopayment.config;

public final class RabbitTopology {

    public static final String PAYMENTS_EXCHANGE = "mercado.payments";
    public static final String RETRY_EXCHANGE = "mercado.payments.retry";
    public static final String DEAD_LETTER_EXCHANGE = "mercado.payments.dlx";

    public static final String REQUEST_QUEUE = "mercado.payment.requests";
    public static final String RETRY_QUEUE = "mercado.payment.requests.retry";
    public static final String DEAD_LETTER_QUEUE = "mercado.payment.requests.dlq";
    public static final String RESULT_QUEUE = "mercado.payment.results";

    public static final String REQUEST_ROUTING_KEY = "payment.request";
    public static final String RETRY_ROUTING_KEY = "payment.request.retry";
    public static final String DEAD_LETTER_ROUTING_KEY = "payment.request.dead";
    public static final String RESULT_ROUTING_KEY = "payment.result";

    public static final String RETRY_COUNT_HEADER = "x-payment-retry-count";
    public static final String LAST_ERROR_HEADER = "x-payment-last-error";

    private RabbitTopology() {
    }
}
