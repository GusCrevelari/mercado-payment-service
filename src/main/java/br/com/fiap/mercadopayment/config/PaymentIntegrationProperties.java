package br.com.fiap.mercadopayment.config;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.payment")
public class PaymentIntegrationProperties {

    private URI bankBaseUrl = URI.create("http://localhost:8091");
    private Duration bankTimeout = Duration.ofSeconds(3);
    private int maxRetries = 3;
    private Duration retryDelay = Duration.ofSeconds(5);

    public URI getBankBaseUrl() {
        return bankBaseUrl;
    }

    public void setBankBaseUrl(URI bankBaseUrl) {
        this.bankBaseUrl = bankBaseUrl;
    }

    public Duration getBankTimeout() {
        return bankTimeout;
    }

    public void setBankTimeout(Duration bankTimeout) {
        this.bankTimeout = bankTimeout;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Duration getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(Duration retryDelay) {
        this.retryDelay = retryDelay;
    }
}
