package com.bankstatement.config;

import com.bankstatement.entity.BankTemplate;
import com.bankstatement.entity.Role;
import com.bankstatement.entity.User;
import com.bankstatement.repository.BankTemplateRepository;
import com.bankstatement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BankTemplateRepository bankTemplateRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUsers();
        seedTemplates();
        ensureBoiTemplate();
    }

    private void seedUsers() {
        if (userRepository.count() > 0) return;

        userRepository.save(User.builder()
                .name("Admin User")
                .email("admin@bankdemo.com")
                .mobile("9876543210")
                .passwordHash(passwordEncoder.encode("Admin@123"))
                .role(Role.ADMIN)
                .active(true)
                .build());

        userRepository.save(User.builder()
                .name("Staff User")
                .email("staff@bankdemo.com")
                .mobile("9876543211")
                .passwordHash(passwordEncoder.encode("Staff@123"))
                .role(Role.STAFF)
                .active(true)
                .build());

        userRepository.save(User.builder()
                .name("Viewer User")
                .email("viewer@bankdemo.com")
                .mobile("9876543212")
                .passwordHash(passwordEncoder.encode("Viewer@123"))
                .role(Role.VIEWER)
                .active(true)
                .build());
    }

    private void seedTemplates() {
        if (bankTemplateRepository.count() > 0) return;

        String[][] banks = {
                {"SBI", "State Bank of India", "true"},
                {"HDFC", "HDFC Bank", "true"},
                {"ICICI", "ICICI Bank", "true"},
                {"AXIS", "Axis Bank", "true"},
                {"KOTAK", "Kotak Mahindra Bank", "true"},
                {"BOI", "Bank of India", "true"},
                {"CANARA", "Canara Bank", "true"}
        };

        for (String[] bank : banks) {
            bankTemplateRepository.save(BankTemplate.builder()
                    .code(bank[0])
                    .displayName(bank[1])
                    .htmlTemplatePath("templates/banks/" + bank[0].toLowerCase() + "/statement.html")
                    .active(Boolean.parseBoolean(bank[2]))
                    .build());
        }
    }

    private void ensureBoiTemplate() {
        if (bankTemplateRepository.findByCode("BOI").isEmpty()) {
            bankTemplateRepository.save(BankTemplate.builder()
                    .code("BOI")
                    .displayName("Bank of India")
                    .htmlTemplatePath("templates/banks/boi/statement.html")
                    .active(true)
                    .build());
        }
    }
}
