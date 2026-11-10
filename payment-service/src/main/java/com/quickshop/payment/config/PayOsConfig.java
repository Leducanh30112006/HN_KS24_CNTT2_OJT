package com.quickshop.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;

@Getter
@Setter
@Configuration
public class PayOsConfig {

    @Value("${payos.client-id:${payos.clientId:}}")
    private String clientId;

    @Value("${payos.api-key:}")
    private String apiKey;

    @Value("${payos.checksum-key:${payos.check-sum:}}")
    private String checksumKey;

    @Bean
    public PayOS payOS() {
        if (clientId != null && !clientId.isBlank() && apiKey != null && !apiKey.isBlank() && checksumKey != null && !checksumKey.isBlank()) {
            return new PayOS(clientId, apiKey, checksumKey);
        }
        return new PayOS("dummy-client-id", "dummy-api-key", "dummy-checksum-key");
    }
}
