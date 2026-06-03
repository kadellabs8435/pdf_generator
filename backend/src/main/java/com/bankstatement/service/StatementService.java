package com.bankstatement.service;

import com.bankstatement.dto.PdfDownloadResult;
import com.bankstatement.dto.StatementDraftRequest;
import com.bankstatement.dto.StatementResponse;
import com.bankstatement.entity.Role;
import com.bankstatement.entity.Statement;
import com.bankstatement.entity.StatementStatus;
import com.bankstatement.entity.User;
import com.bankstatement.exception.ApiException;
import com.bankstatement.repository.StatementRepository;
import com.bankstatement.security.UserPrincipal;
import com.bankstatement.service.admin.ActivityLogService;
import com.bankstatement.service.pdf.HtmlPdfRendererService;
import com.bankstatement.service.pdf.PdfGeneratorService;
import com.bankstatement.service.template.TemplateService;
import com.bankstatement.service.transaction.TransactionAmountGuard;
import com.bankstatement.service.transaction.TransactionGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatementService {

    private final StatementRepository statementRepository;
    private final TemplateService templateService;
    private final TransactionGeneratorService transactionGeneratorService;
    private final PdfGeneratorService pdfGeneratorService;
    private final HtmlPdfRendererService htmlPdfRendererService;
    private final ActivityLogService activityLogService;

    public StatementResponse createDraft(StatementDraftRequest request) {
        UserPrincipal principal = currentUser();
        templateService.getEntityByCode(request.bankCode());
        validateBoiCustomerDetails(request);
        validateSalarySettings(request.transactionSettings());

        Statement statement = Statement.builder()
                .createdByUserId(principal.getUser().getId())
                .bankCode(request.bankCode().toUpperCase())
                .status(StatementStatus.DRAFT)
                .customerDetails(request.customerDetails())
                .accountDetails(request.accountDetails())
                .period(request.period())
                .openingBalance(request.openingBalance())
                .transactionSettings(request.transactionSettings())
                .build();

        Statement saved = statementRepository.save(statement);
        logActivity(principal.getUser(), "CREATE_DRAFT", "Statement draft created", saved.getId());
        return toResponse(saved);
    }

    public StatementResponse generateTransactions(String id) {
        Statement statement = getAccessibleStatement(id);
        List<com.bankstatement.entity.Transaction> transactions = transactionGeneratorService.generate(statement);
        statement.setTransactions(transactions);
        if (!transactions.isEmpty()) {
            statement.setClosingBalance(transactions.get(transactions.size() - 1).getBalance());
        } else {
            statement.setClosingBalance(statement.getOpeningBalance());
        }
        statement.setStatus(StatementStatus.PREVIEWED);
        Statement saved = statementRepository.save(statement);
        logActivity(currentUser().getUser(), "GENERATE_TRANSACTIONS", "Transactions generated", saved.getId());
        return toResponse(saved);
    }

    public byte[] previewPdf(String id) {
        Statement statement = getAccessibleStatement(id);
        if (statement.getTransactions() == null || statement.getTransactions().isEmpty()) {
            throw new ApiException("Generate transactions first", HttpStatus.BAD_REQUEST.value());
        }
        byte[] pdf = pdfGeneratorService.renderPreview(statement);
        statement.setStatus(StatementStatus.PREVIEWED);
        statementRepository.save(statement);
        logActivity(currentUser().getUser(), "PREVIEW_PDF", "PDF preview generated", statement.getId());
        return pdf;
    }

    /**
     * Converts frontend-rendered HTML layout to PDF (migration path).
     * Does not replace legacy iText preview/download — BOI download encryption unchanged.
     */
    public byte[] previewLayoutPdf(String id, String html) {
        Statement statement = getAccessibleStatement(id);
        if (statement.getTransactions() == null || statement.getTransactions().isEmpty()) {
            throw new ApiException("Generate transactions first", HttpStatus.BAD_REQUEST.value());
        }
        String bank = statement.getBankCode() != null ? statement.getBankCode().toUpperCase() : "";
        if (!List.of("SBI", "KOTAK", "BOI").contains(bank)) {
            throw new ApiException("Layout PDF preview not supported for bank: " + bank,
                    HttpStatus.BAD_REQUEST.value());
        }
        byte[] pdf = htmlPdfRendererService.renderHtml(html);
        logActivity(currentUser().getUser(), "PREVIEW_LAYOUT_PDF", "Layout PDF preview from frontend HTML", statement.getId());
        return pdf;
    }

    public PdfDownloadResult downloadPdf(String id) {
        Statement statement = getAccessibleStatement(id);
        if (statement.getTransactions() == null || statement.getTransactions().isEmpty()) {
            throw new ApiException("Generate transactions first", HttpStatus.BAD_REQUEST.value());
        }
        byte[] pdf = pdfGeneratorService.renderAndStore(statement);
        statement.setStatus(StatementStatus.GENERATED);
        statementRepository.save(statement);
        logActivity(currentUser().getUser(), "DOWNLOAD_PDF", "PDF downloaded", statement.getId());
        return new PdfDownloadResult(pdf, pdfGeneratorService.resolveDownloadFilename(statement));
    }

    public StatementResponse approve(String id) {
        UserPrincipal principal = currentUser();
        if (principal.getUser().getRole() == Role.VIEWER) {
            throw new ApiException("Not authorized to approve", HttpStatus.FORBIDDEN.value());
        }
        Statement statement = getAccessibleStatement(id);
        if (statement.getStatus() != StatementStatus.GENERATED && statement.getStatus() != StatementStatus.PREVIEWED) {
            throw new ApiException("Statement must be generated before approval", HttpStatus.BAD_REQUEST.value());
        }
        statement.setStatus(StatementStatus.APPROVED);
        Statement saved = statementRepository.save(statement);
        logActivity(principal.getUser(), "APPROVE_STATEMENT", "Statement approved", saved.getId());
        return toResponse(saved);
    }

    public StatementResponse getById(String id) {
        return toResponse(getAccessibleStatement(id));
    }

    public Page<StatementResponse> getHistory(String bankCode, StatementStatus status, Pageable pageable) {
        UserPrincipal principal = currentUser();
        User user = principal.getUser();
        Page<Statement> page;

        if (user.getRole() == Role.VIEWER) {
            page = statementRepository.findByStatus(StatementStatus.APPROVED, pageable);
        } else if (user.getRole() == Role.ADMIN) {
            if (bankCode != null && status != null) {
                page = statementRepository.findByBankCode(bankCode.toUpperCase(), pageable)
                        .map(s -> s); // fallback - simplified filtering below
                page = filterPage(page, bankCode, status);
            } else if (status != null) {
                page = statementRepository.findByStatus(status, pageable);
            } else if (bankCode != null) {
                page = statementRepository.findByBankCode(bankCode.toUpperCase(), pageable);
            } else {
                page = statementRepository.findAll(pageable);
            }
        } else {
            if (status != null) {
                page = statementRepository.findByCreatedByUserIdAndStatus(user.getId(), status, pageable);
            } else {
                page = statementRepository.findByCreatedByUserId(user.getId(), pageable);
            }
            if (bankCode != null) {
                page = filterPage(page, bankCode, null);
            }
        }

        return page.map(this::toResponse);
    }

    private Page<Statement> filterPage(Page<Statement> page, String bankCode, StatementStatus status) {
        List<Statement> filtered = page.getContent().stream()
                .filter(s -> bankCode == null || s.getBankCode().equalsIgnoreCase(bankCode))
                .filter(s -> status == null || s.getStatus() == status)
                .toList();
        return new org.springframework.data.domain.PageImpl<>(filtered, page.getPageable(), filtered.size());
    }

    public Statement getAccessibleStatement(String id) {
        Statement statement = statementRepository.findById(id)
                .orElseThrow(() -> new ApiException("Statement not found", HttpStatus.NOT_FOUND.value()));

        UserPrincipal principal = currentUser();
        User user = principal.getUser();

        if (user.getRole() == Role.VIEWER) {
            if (statement.getStatus() != StatementStatus.APPROVED) {
                throw new ApiException("Access denied", HttpStatus.FORBIDDEN.value());
            }
        } else if (user.getRole() == Role.STAFF) {
            if (!statement.getCreatedByUserId().equals(user.getId())) {
                throw new ApiException("Access denied", HttpStatus.FORBIDDEN.value());
            }
        }

        return statement;
    }

    public Statement save(Statement statement) {
        return statementRepository.save(statement);
    }

    private UserPrincipal currentUser() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private void logActivity(User user, String action, String details, String entityId) {
        activityLogService.log(user.getId(), user.getName(), action, details, "STATEMENT", entityId);
    }

    private void validateBoiCustomerDetails(StatementDraftRequest request) {
        if (!"BOI".equalsIgnoreCase(request.bankCode())) {
            return;
        }
        if (request.customerDetails() == null
                || request.customerDetails().getCustomerName() == null
                || request.customerDetails().getCustomerName().isBlank()) {
            throw new ApiException("Customer name is required for Bank of India statements",
                    HttpStatus.BAD_REQUEST.value());
        }
        if (request.customerDetails().getDateOfBirth() == null) {
            throw new ApiException("Date of birth is required for Bank of India statements",
                    HttpStatus.BAD_REQUEST.value());
        }
        if (request.customerDetails().getCustomerId() == null
                || request.customerDetails().getCustomerId().isBlank()) {
            throw new ApiException("Customer ID is required for Bank of India statements",
                    HttpStatus.BAD_REQUEST.value());
        }
    }

    private void validateSalarySettings(com.bankstatement.entity.TransactionSettings settings) {
        if (settings == null || !settings.isSalary()) {
            return;
        }
        if (settings.getSalaryCompanyName() == null || settings.getSalaryCompanyName().isBlank()) {
            throw new ApiException("Company name is required when salary is selected",
                    HttpStatus.BAD_REQUEST.value());
        }
        if (settings.getSalaryAmount() == null || settings.getSalaryAmount().signum() <= 0) {
            throw new ApiException("Salary amount is required when salary is selected",
                    HttpStatus.BAD_REQUEST.value());
        }
        Integer day = settings.getSalaryDayOfMonth();
        if (day == null || day < 1 || day > 28) {
            throw new ApiException("Salary credit day (1–28) is required when salary is selected",
                    HttpStatus.BAD_REQUEST.value());
        }
    }

    public StatementResponse toResponse(Statement statement) {
        TransactionAmountGuard.prepareForRender(
                statement.getTransactions(), statement.getOpeningBalance());
        return new StatementResponse(
                statement.getId(),
                statement.getBankCode(),
                statement.getStatus(),
                statement.getCustomerDetails(),
                statement.getAccountDetails(),
                statement.getPeriod(),
                statement.getOpeningBalance(),
                statement.getClosingBalance(),
                statement.getTransactionSettings(),
                statement.getTransactions(),
                statement.getPdfPath(),
                statement.getCreatedByUserId(),
                statement.getCreatedAt(),
                statement.getUpdatedAt()
        );
    }
}
