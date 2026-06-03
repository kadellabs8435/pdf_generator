package com.bankstatement.service.template;

import com.bankstatement.dto.BankTemplateRequest;
import com.bankstatement.dto.BankTemplateResponse;
import com.bankstatement.entity.BankTemplate;
import com.bankstatement.exception.ApiException;
import com.bankstatement.repository.BankTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private static final Map<String, String> DEFAULT_TEMPLATE_PATHS = Map.of(
            "SBI", "templates/banks/sbi/statement.html",
            "HDFC", "templates/banks/hdfc/statement.html",
            "ICICI", "templates/banks/icici/statement.html",
            "AXIS", "templates/banks/axis/statement.html",
            "KOTAK", "templates/banks/kotak/statement.html",
            "CANARA", "templates/banks/canara/statement.html"
    );

    private final BankTemplateRepository bankTemplateRepository;

    public List<BankTemplateResponse> getActiveTemplates() {
        return bankTemplateRepository.findByActiveTrue().stream().map(this::toResponse).toList();
    }

    public List<BankTemplateResponse> getAllTemplates() {
        return bankTemplateRepository.findAll().stream().map(this::toResponse).toList();
    }

    public BankTemplateResponse getByCode(String code) {
        return bankTemplateRepository.findByCode(code.toUpperCase())
                .map(this::toResponse)
                .orElseThrow(() -> new ApiException("Template not found", HttpStatus.NOT_FOUND.value()));
    }

    public BankTemplate getEntityByCode(String code) {
        return bankTemplateRepository.findByCode(code.toUpperCase())
                .filter(BankTemplate::isActive)
                .orElseThrow(() -> new ApiException("Template not found or inactive", HttpStatus.NOT_FOUND.value()));
    }

    public BankTemplateResponse create(BankTemplateRequest request) {
        String code = request.code().toUpperCase();
        if (bankTemplateRepository.findByCode(code).isPresent()) {
            throw new ApiException("Template code already exists", HttpStatus.CONFLICT.value());
        }

        BankTemplate template = BankTemplate.builder()
                .code(code)
                .displayName(request.displayName())
                .htmlTemplatePath(DEFAULT_TEMPLATE_PATHS.getOrDefault(code, "templates/banks/sbi/statement.html"))
                .active(request.active())
                .build();

        return toResponse(bankTemplateRepository.save(template));
    }

    public BankTemplateResponse update(String id, BankTemplateRequest request) {
        BankTemplate template = bankTemplateRepository.findById(id)
                .orElseThrow(() -> new ApiException("Template not found", HttpStatus.NOT_FOUND.value()));

        template.setDisplayName(request.displayName());
        template.setActive(request.active());

        return toResponse(bankTemplateRepository.save(template));
    }

    public void delete(String id) {
        if (!bankTemplateRepository.existsById(id)) {
            throw new ApiException("Template not found", HttpStatus.NOT_FOUND.value());
        }
        bankTemplateRepository.deleteById(id);
    }

    private BankTemplateResponse toResponse(BankTemplate template) {
        return new BankTemplateResponse(template.getId(), template.getCode(), template.getDisplayName(), template.isActive());
    }
}
