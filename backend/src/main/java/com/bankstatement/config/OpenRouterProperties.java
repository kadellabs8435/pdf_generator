package com.bankstatement.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.openrouter")
public class OpenRouterProperties {

    private String apiKey = "";
    private String model = "openai/gpt-4o-mini";
    private double temperature = 0.7;
    private boolean enabled = true;
    private String baseUrl = "https://openrouter.ai/api/v1/chat/completions";

    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }
}
