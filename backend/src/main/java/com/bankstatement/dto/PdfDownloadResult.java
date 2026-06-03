package com.bankstatement.dto;

public record PdfDownloadResult(byte[] pdfBytes, String filename) {}
