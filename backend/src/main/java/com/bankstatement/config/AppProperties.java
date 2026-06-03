package com.bankstatement.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Otp otp = new Otp();
    private Pdf pdf = new Pdf();
    private Cors cors = new Cors();

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long expirationMs;
    }

    @Getter
    @Setter
    public static class Otp {
        private int expiryMinutes;
    }

    @Getter
    @Setter
    public static class Pdf {
        private String storagePath;
    }

    @Getter
    @Setter
    public static class Cors {
        private String allowedOrigins;
    }
}
