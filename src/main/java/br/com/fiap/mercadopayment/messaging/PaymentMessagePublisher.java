package br.com.fiap.mercadopayment.messaging;

import br.com.fiap.mercadopayment.config.RabbitTopology;
import br.com.fiap.mercadopayment.domain.PaymentRequestEnvelope;
import br.com.fiap.mercadopayment.domain.PaymentResultEnvelope;
import java.nio.charset.StandardCharsets;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentMessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    public PaymentMessagePublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishResult(PaymentResultEnvelope result) {
        rabbitTemplate.convertAndSend(
                RabbitTopology.PAYMENTS_EXCHANGE,
                RabbitTopology.RESULT_ROUTING_KEY,
                result,
                message -> {
                    message.getMessageProperties().setMessageId(result.paymentAttemptId().toString());
                    message.getMessageProperties().setCorrelationId(
                            result.payload().correlationId().toString());
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return message;
                });
    }

    public void publishRetry(PaymentRequestEnvelope request, int retryCount, Exception failure) {
        rabbitTemplate.convertAndSend(
                RabbitTopology.RETRY_EXCHANGE,
                RabbitTopology.RETRY_ROUTING_KEY,
                request,
                message -> withFailureMetadata(message, request, retryCount, failure));
    }

    public void publishDeadLetter(PaymentRequestEnvelope request, int retryCount, Exception failure) {
        rabbitTemplate.convertAndSend(
                RabbitTopology.DEAD_LETTER_EXCHANGE,
                RabbitTopology.DEAD_LETTER_ROUTING_KEY,
                request,
                message -> withFailureMetadata(message, request, retryCount, failure));
    }

    private org.springframework.amqp.core.Message withFailureMetadata(
            org.springframework.amqp.core.Message message,
            PaymentRequestEnvelope request,
            int retryCount,
            Exception failure) {
        message.getMessageProperties().setMessageId(request.paymentAttemptId().toString());
        message.getMessageProperties().setCorrelationId(request.payload().correlationId().toString());
        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        message.getMessageProperties().setHeader(RabbitTopology.RETRY_COUNT_HEADER, retryCount);
        String error = failure.getClass().getSimpleName() + ": " + String.valueOf(failure.getMessage());
        byte[] bytes = error.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 512) {
            error = new String(bytes, 0, 512, StandardCharsets.UTF_8);
        }
        message.getMessageProperties().setHeader(RabbitTopology.LAST_ERROR_HEADER, error);
        return message;
    }
}
