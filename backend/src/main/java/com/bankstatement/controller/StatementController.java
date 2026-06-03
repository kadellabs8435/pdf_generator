package com.bankstatement.controller;

import com.bankstatement.dto.BulkJobResponse;
import com.bankstatement.dto.LayoutPdfRequest;
import com.bankstatement.dto.NarrationSuggestionResponse;
import com.bankstatement.dto.StatementDraftRequest;
import com.bankstatement.dto.StatementResponse;
import com.bankstatement.entity.BulkJob;
import com.bankstatement.entity.StatementStatus;
import com.bankstatement.security.UserPrincipal;
import com.bankstatement.service.NarrationSuggestionService;
import com.bankstatement.service.StatementService;
import com.bankstatement.service.excel.ExcelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/statements")
@RequiredArgsConstructor
public class StatementController {

    private final StatementService statementService;
    private final ExcelService excelService;
    private final NarrationSuggestionService narrationSuggestionService;

    @PostMapping("/draft")
    public StatementResponse createDraft(@Valid @RequestBody StatementDraftRequest request) {
        return statementService.createDraft(request);
    }

    @PostMapping("/{id}/generate-transactions")
    public StatementResponse generateTransactions(@PathVariable String id) {
        return statementService.generateTransactions(id);
    }

    @PostMapping("/{id}/preview")
    public ResponseEntity<byte[]> preview(@PathVariable String id) {
        byte[] pdf = statementService.previewPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=statement-preview.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/{id}/preview-layout")
    public ResponseEntity<byte[]> previewLayout(
            @PathVariable String id,
            @Valid @RequestBody LayoutPdfRequest request
    ) {
        byte[] pdf = statementService.previewLayoutPdf(id, request.html());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=statement-layout-preview.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable String id) {
        var result = statementService.downloadPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + result.filename() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(result.pdfBytes());
    }

    @PostMapping("/{id}/approve")
    public StatementResponse approve(@PathVariable String id) {
        return statementService.approve(id);
    }

    @GetMapping("/history")
    public Page<StatementResponse> history(
            @RequestParam(required = false) String bankCode,
            @RequestParam(required = false) StatementStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return statementService.getHistory(bankCode, status, PageRequest.of(page, size));
    }

    @GetMapping("/{id}")
    public StatementResponse getById(@PathVariable String id) {
        return statementService.getById(id);
    }

    @GetMapping("/bulk-template")
    public ResponseEntity<byte[]> bulkTemplate() {
        byte[] data = excelService.downloadTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bulk-statement-template.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @PostMapping("/bulk-upload")
    public BulkJobResponse bulkUpload(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        BulkJob job = excelService.processBulkUpload(file, principal.getUser().getId());
        return toBulkResponse(job);
    }

    @GetMapping("/bulk-jobs/{jobId}")
    public BulkJobResponse bulkJobStatus(@PathVariable String jobId) {
        return toBulkResponse(excelService.getJob(jobId));
    }

    @PostMapping("/suggest-narrations")
    public NarrationSuggestionResponse suggestNarrations(@RequestBody Map<String, String> body) {
        return narrationSuggestionService.suggest(body.get("context"), body.get("transactionType"));
    }

    private BulkJobResponse toBulkResponse(BulkJob job) {
        return new BulkJobResponse(
                job.getId(), job.getStatus(), job.getTotalRows(), job.getProcessedRows(),
                job.getSuccessCount(), job.getFailureCount(), job.getErrorReport(),
                job.getCreatedAt(), job.getCompletedAt()
        );
    }
}
