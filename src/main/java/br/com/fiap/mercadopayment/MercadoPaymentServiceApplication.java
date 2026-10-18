package br.com.fiap.mercadopayment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MercadoPaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MercadoPaymentServiceApplication.class, args);
    }
}
