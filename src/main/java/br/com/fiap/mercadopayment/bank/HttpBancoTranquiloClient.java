package br.com.fiap.mercadopayment.bank;

import br.com.fiap.mercadopayment.config.PaymentIntegrationProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.springframework.stereotype.Component;

@Component
public class HttpBancoTranquiloClient implements BancoTranquiloClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final PaymentIntegrationProperties properties;

    public HttpBancoTranquiloClient(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            PaymentIntegrationProperties properties) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @Override
    public BankPaymentResponse authorize(BankPaymentRequest request) {
        try {
            URI endpoint = properties.getBankBaseUrl().resolve("/api/v1/payments");
            HttpRequest httpRequest = HttpRequest.newBuilder(endpoint)
                    .timeout(properties.getBankTimeout())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(request)))
                    .build();
            HttpResponse<String> response = httpClient.send(
                    httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BancoTranquiloClientException(
                        "Banco Tranquilo respondeu HTTP " + response.statusCode());
            }
            return objectMapper.readValue(response.body(), BankPaymentResponse.class);
        } catch (JsonProcessingException exception) {
            throw new BancoTranquiloClientException("Resposta JSON invalida do Banco Tranquilo.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BancoTranquiloClientException("Chamada ao Banco Tranquilo interrompida.", exception);
        } catch (IOException exception) {
            throw new BancoTranquiloClientException("Banco Tranquilo indisponivel.", exception);
        }
    }
}
