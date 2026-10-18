package br.com.fiap.mercadopayment.messaging;

import br.com.fiap.mercadopayment.config.PaymentIntegrationProperties;
import br.com.fiap.mercadopayment.config.RabbitTopology;
import br.com.fiap.mercadopayment.domain.PaymentRequestEnvelope;
import br.com.fiap.mercadopayment.domain.PaymentResultEnvelope;
import br.com.fiap.mercadopayment.service.PaymentProcessor;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentRequestListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentRequestListener.class);

    private final PaymentProcessor processor;
    private final PaymentMessagePublisher publisher;
    private final PaymentIntegrationProperties properties;
    private final Validator validator;

    public PaymentRequestListener(
            PaymentProcessor processor,
            PaymentMessagePublisher publisher,
            PaymentIntegrationProperties properties,
            Validator validator) {
        this.processor = processor;
        this.publisher = publisher;
        this.properties = properties;
        this.validator = validator;
    }

    @RabbitListener(queues = RabbitTopology.REQUEST_QUEUE)
    public void receive(PaymentRequestEnvelope envelope, Message sourceMessage) {
        int retryCount = readRetryCount(sourceMessage);
        try {
            validate(envelope);
            var processed = processor.process(envelope);
            publisher.publishResult(new PaymentResultEnvelope(
                    envelope.paymentAttemptId(), processed.result()));
            LOGGER.info("Pagamento {} processado; duplicate={}",
                    envelope.paymentAttemptId(), processed.duplicate());
        } catch (Exception failure) {
            handleFailure(envelope, retryCount, failure);
        }
    }

    private void validate(PaymentRequestEnvelope envelope) {
        Set<ConstraintViolation<PaymentRequestEnvelope>> violations = validator.validate(envelope);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(violations.iterator().next().getMessage());
        }
    }

    private int readRetryCount(Message message) {
        Object value = message.getMessageProperties().getHeaders().get(RabbitTopology.RETRY_COUNT_HEADER);
        return value instanceof Number number ? number.intValue() : 0;
    }

    private void handleFailure(PaymentRequestEnvelope envelope, int retryCount, Exception failure) {
        if (envelope != null
                && envelope.paymentAttemptId() != null
                && envelope.payload() != null
                && envelope.payload().correlationId() != null
                && retryCount < properties.getMaxRetries()) {
            int nextRetry = retryCount + 1;
            publisher.publishRetry(envelope, nextRetry, failure);
            LOGGER.warn("Pagamento {} enviado para retry {}/{}: {}",
                    envelope.paymentAttemptId(), nextRetry, properties.getMaxRetries(), failure.getMessage());
            return;
        }
        if (envelope != null
                && envelope.paymentAttemptId() != null
                && envelope.payload() != null
                && envelope.payload().correlationId() != null) {
            publisher.publishDeadLetter(envelope, retryCount, failure);
            LOGGER.error("Pagamento {} enviado para DLQ apos {} retries.",
                    envelope.paymentAttemptId(), retryCount, failure);
            return;
        }
        throw new IllegalArgumentException("Envelope invalido e sem identificadores para republicacao.", failure);
    }
}
