package com.bankstatement.service.excel;

import com.bankstatement.dto.StatementDraftRequest;
import com.bankstatement.entity.*;
import com.bankstatement.exception.ApiException;
import com.bankstatement.repository.BulkJobRepository;
import com.bankstatement.service.StatementService;
import com.bankstatement.service.template.TemplateService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final BulkJobRepository bulkJobRepository;
    private final StatementService statementService;
    private final TemplateService templateService;

    public byte[] downloadTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Statements");
            Row header = sheet.createRow(0);
            String[] columns = {
                    "bankCode", "customerName", "address", "city", "state", "pincode",
                    "accountNumber", "accountType", "branchName", "ifscCode",
                    "fromDate", "toDate", "openingBalance",
                    "salary", "upi", "atm", "emi", "interest", "minTransactions", "maxTransactions"
            };
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }
            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("SBI");
            sample.createCell(1).setCellValue("John Doe");
            sample.createCell(2).setCellValue("123 Main St");
            sample.createCell(3).setCellValue("Mumbai");
            sample.createCell(4).setCellValue("Maharashtra");
            sample.createCell(5).setCellValue("400001");
            sample.createCell(6).setCellValue("12345678901234");
            sample.createCell(7).setCellValue("Savings");
            sample.createCell(8).setCellValue("Andheri Branch");
            sample.createCell(9).setCellValue("SBIN0001234");
            sample.createCell(10).setCellValue("01-01-2025");
            sample.createCell(11).setCellValue("31-01-2025");
            sample.createCell(12).setCellValue(50000);
            sample.createCell(13).setCellValue(true);
            sample.createCell(14).setCellValue(true);
            sample.createCell(15).setCellValue(true);
            sample.createCell(16).setCellValue(true);
            sample.createCell(17).setCellValue(true);
            sample.createCell(18).setCellValue(8);
            sample.createCell(19).setCellValue(20);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ApiException("Failed to generate template", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    public BulkJob processBulkUpload(MultipartFile file, String userId) {
        BulkJob job = BulkJob.builder()
                .userId(userId)
                .status("PROCESSING")
                .totalRows(0)
                .processedRows(0)
                .successCount(0)
                .failureCount(0)
                .build();
        job = bulkJobRepository.save(job);

        List<String> errors = new ArrayList<>();
        int success = 0;
        int failure = 0;

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            job.setTotalRows(sheet.getLastRowNum());
            bulkJobRepository.save(job);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                try {
                    StatementDraftRequest request = parseRow(row);
                    templateService.getEntityByCode(request.bankCode());
                    var draft = statementService.createDraft(request);
                    var withTxn = statementService.generateTransactions(draft.id());
                    statementService.downloadPdf(withTxn.id());
                    success++;
                } catch (Exception ex) {
                    failure++;
                    errors.add("Row " + (i + 1) + ": " + ex.getMessage());
                }
                job.setProcessedRows(i);
                job.setSuccessCount(success);
                job.setFailureCount(failure);
                bulkJobRepository.save(job);
            }

            job.setStatus(failure == 0 ? "COMPLETED" : "COMPLETED_WITH_ERRORS");
            job.setErrorReport(String.join("\n", errors));
            job.setCompletedAt(java.time.Instant.now());
            bulkJobRepository.save(job);
            return job;
        } catch (Exception e) {
            job.setStatus("FAILED");
            job.setErrorReport(e.getMessage());
            job.setCompletedAt(java.time.Instant.now());
            bulkJobRepository.save(job);
            throw new ApiException("Bulk upload failed: " + e.getMessage(), HttpStatus.BAD_REQUEST.value());
        }
    }

    public BulkJob getJob(String id) {
        return bulkJobRepository.findById(id)
                .orElseThrow(() -> new ApiException("Bulk job not found", HttpStatus.NOT_FOUND.value()));
    }

    private StatementDraftRequest parseRow(Row row) {
        return new StatementDraftRequest(
                getString(row, 0),
                CustomerDetails.builder()
                        .customerName(getString(row, 1))
                        .address(getString(row, 2))
                        .city(getString(row, 3))
                        .state(getString(row, 4))
                        .pincode(getString(row, 5))
                        .build(),
                AccountDetails.builder()
                        .accountNumber(getString(row, 6))
                        .accountType(getString(row, 7))
                        .branchName(getString(row, 8))
                        .ifscCode(getString(row, 9))
                        .build(),
                StatementPeriod.builder()
                        .fromDate(parseDate(getString(row, 10)))
                        .toDate(parseDate(getString(row, 11)))
                        .build(),
                BigDecimal.valueOf(getNumeric(row, 12)),
                TransactionSettings.builder()
                        .salary(getBoolean(row, 13))
                        .upi(getBoolean(row, 14))
                        .atm(getBoolean(row, 15))
                        .emi(getBoolean(row, 16))
                        .interest(getBoolean(row, 17))
                        .minTransactions((int) getNumeric(row, 18))
                        .maxTransactions((int) getNumeric(row, 19))
                        .build()
        );
    }

    private String getString(Row row, int idx) {
        Cell cell = row.getCell(idx);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private double getNumeric(Row row, int idx) {
        Cell cell = row.getCell(idx);
        if (cell == null) return 0;
        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING -> {
                try {
                    yield Double.parseDouble(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    yield 0;
                }
            }
            default -> 0;
        };
    }

    private boolean getBoolean(Row row, int idx) {
        Cell cell = row.getCell(idx);
        if (cell == null) return false;
        return switch (cell.getCellType()) {
            case BOOLEAN -> cell.getBooleanCellValue();
            case STRING -> Boolean.parseBoolean(cell.getStringCellValue().trim());
            case NUMERIC -> cell.getNumericCellValue() > 0;
            default -> false;
        };
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            throw new ApiException("Date is required", HttpStatus.BAD_REQUEST.value());
        }
        return LocalDate.parse(value, DATE_FMT);
    }
}
