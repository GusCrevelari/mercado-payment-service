package br.com.fiap.mercadopayment.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;

class RabbitMqConfigTest {

    private final RabbitMqConfig config = new RabbitMqConfig();

    @Test
    void shouldDeclareDurableMainResultAndDeadLetterQueues() {
        assertDurable(config.requestQueue());
        assertDurable(config.resultQueue());
        assertDurable(config.deadLetterQueue());
        assertThat(config.paymentsExchange().isDurable()).isTrue();
        assertThat(config.deadLetterExchange().isDurable()).isTrue();
    }

    @Test
    void shouldReturnRetryMessageToMainExchangeAfterConfiguredTtl() {
        PaymentIntegrationProperties properties = new PaymentIntegrationProperties();
        properties.setRetryDelay(Duration.ofSeconds(7));

        Queue retry = config.retryQueue(properties);

        assertDurable(retry);
        assertThat(retry.getArguments())
                .containsEntry("x-message-ttl", 7000)
                .containsEntry("x-dead-letter-exchange", RabbitTopology.PAYMENTS_EXCHANGE)
                .containsEntry("x-dead-letter-routing-key", RabbitTopology.REQUEST_ROUTING_KEY);
    }

    private void assertDurable(Queue queue) {
        assertThat(queue.isDurable()).isTrue();
        assertThat(queue.isAutoDelete()).isFalse();
    }
}
