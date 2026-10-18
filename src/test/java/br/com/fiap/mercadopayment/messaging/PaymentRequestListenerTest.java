package br.com.fiap.mercadopayment.messaging;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fiap.mercadopayment.config.PaymentIntegrationProperties;
import br.com.fiap.mercadopayment.config.RabbitTopology;
import br.com.fiap.mercadopayment.domain.PaymentRequest;
import br.com.fiap.mercadopayment.domain.PaymentRequestEnvelope;
import br.com.fiap.mercadopayment.domain.PaymentResult;
import br.com.fiap.mercadopayment.domain.PaymentStatus;
import br.com.fiap.mercadopayment.service.PaymentProcessor;
import br.com.fiap.mercadopayment.service.ProcessedPayment;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

@ExtendWith(MockitoExtension.class)
class PaymentRequestListenerTest {

    @Mock
    private PaymentProcessor processor;

    @Mock
    private PaymentMessagePublisher publisher;

    private PaymentIntegrationProperties properties;
    private Validator validator;

    @BeforeEach
    void setUp() {
        properties = new PaymentIntegrationProperties();
        properties.setMaxRetries(2);
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldPublishResultAfterSuccess() {
        PaymentRequestEnvelope envelope = envelope();
        PaymentResult result = new PaymentResult(
                envelope.payload().purchaseId(), envelope.payload().correlationId(),
                envelope.payload().amount(), "BRL", PaymentStatus.APPROVED);
        when(processor.process(envelope)).thenReturn(new ProcessedPayment(result, false));
        PaymentRequestListener listener = new PaymentRequestListener(
                processor, publisher, properties, validator);

        listener.receive(envelope, new Message(new byte[0], new MessageProperties()));

        verify(publisher).publishResult(new br.com.fiap.mercadopayment.domain.PaymentResultEnvelope(
                envelope.paymentAttemptId(), result));
        verify(publisher, never()).publishRetry(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldSendToRetryAndThenToDeadLetterQueue() {
        PaymentRequestEnvelope envelope = envelope();
        IllegalStateException failure = new IllegalStateException("bank offline");
        when(processor.process(envelope)).thenThrow(failure);
        PaymentRequestListener listener = new PaymentRequestListener(
                processor, publisher, properties, validator);

        listener.receive(envelope, messageWithRetry(0));
        listener.receive(envelope, messageWithRetry(2));

        verify(publisher).publishRetry(envelope, 1, failure);
        verify(publisher).publishDeadLetter(envelope, 2, failure);
    }

    private PaymentRequestEnvelope envelope() {
        return new PaymentRequestEnvelope(
                UUID.randomUUID(),
                new PaymentRequest(11L, UUID.randomUUID(), new BigDecimal("39.90"), "BRL"));
    }

    private Message messageWithRetry(int retryCount) {
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setHeader(RabbitTopology.RETRY_COUNT_HEADER, retryCount);
        return new Message(new byte[0], messageProperties);
    }
}
