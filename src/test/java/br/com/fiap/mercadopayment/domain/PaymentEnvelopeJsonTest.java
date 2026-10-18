package br.com.fiap.mercadopayment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentEnvelopeJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void shouldRoundTripMercadoFieldsInsideStableEnvelope() throws Exception {
        PaymentRequestEnvelope original = new PaymentRequestEnvelope(
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
                new PaymentRequest(
                        11L,
                        UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                        new BigDecimal("39.90"),
                        "BRL"));

        String json = objectMapper.writeValueAsString(original);
        PaymentRequestEnvelope restored = objectMapper.readValue(json, PaymentRequestEnvelope.class);

        assertThat(restored).isEqualTo(original);
        assertThat(json).contains("paymentAttemptId", "purchaseId", "correlationId", "amount", "currency");
    }
}
