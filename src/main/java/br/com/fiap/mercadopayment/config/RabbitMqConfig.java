package br.com.fiap.mercadopayment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    DirectExchange paymentsExchange() {
        return ExchangeBuilder.directExchange(RabbitTopology.PAYMENTS_EXCHANGE).durable(true).build();
    }

    @Bean
    DirectExchange retryExchange() {
        return ExchangeBuilder.directExchange(RabbitTopology.RETRY_EXCHANGE).durable(true).build();
    }

    @Bean
    DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(RabbitTopology.DEAD_LETTER_EXCHANGE).durable(true).build();
    }

    @Bean
    Queue requestQueue() {
        return QueueBuilder.durable(RabbitTopology.REQUEST_QUEUE)
                .deadLetterExchange(RabbitTopology.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(RabbitTopology.DEAD_LETTER_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue retryQueue(PaymentIntegrationProperties properties) {
        return QueueBuilder.durable(RabbitTopology.RETRY_QUEUE)
                .ttl((int) properties.getRetryDelay().toMillis())
                .deadLetterExchange(RabbitTopology.PAYMENTS_EXCHANGE)
                .deadLetterRoutingKey(RabbitTopology.REQUEST_ROUTING_KEY)
                .build();
    }

    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable(RabbitTopology.DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Queue resultQueue() {
        return QueueBuilder.durable(RabbitTopology.RESULT_QUEUE).build();
    }

    @Bean
    Binding requestBinding(Queue requestQueue, DirectExchange paymentsExchange) {
        return BindingBuilder.bind(requestQueue).to(paymentsExchange).with(RabbitTopology.REQUEST_ROUTING_KEY);
    }

    @Bean
    Binding retryBinding(Queue retryQueue, DirectExchange retryExchange) {
        return BindingBuilder.bind(retryQueue).to(retryExchange).with(RabbitTopology.RETRY_ROUTING_KEY);
    }

    @Bean
    Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange)
                .with(RabbitTopology.DEAD_LETTER_ROUTING_KEY);
    }

    @Bean
    Binding resultBinding(Queue resultQueue, DirectExchange paymentsExchange) {
        return BindingBuilder.bind(resultQueue).to(paymentsExchange).with(RabbitTopology.RESULT_ROUTING_KEY);
    }

    @Bean
    MessageConverter messageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setMandatory(true);
        template.setBeforePublishPostProcessors(message -> {
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            return message;
        });
        return template;
    }

    @Bean
    HttpClient bankHttpClient(PaymentIntegrationProperties properties) {
        return HttpClient.newBuilder()
                .connectTimeout(properties.getBankTimeout())
                .build();
    }
}
