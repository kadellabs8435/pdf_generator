package com.bankstatement;

import com.bankstatement.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(AppProperties.class)
public class BankStatementApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankStatementApplication.class, args);
    }
}
