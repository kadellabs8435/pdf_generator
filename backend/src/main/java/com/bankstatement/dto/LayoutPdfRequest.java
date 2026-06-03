package com.bankstatement.dto;

import jakarta.validation.constraints.NotBlank;

/** HTML snapshot from frontend React layout for PDF export (migration path). */
public record LayoutPdfRequest(
        @NotBlank String html
) {}
