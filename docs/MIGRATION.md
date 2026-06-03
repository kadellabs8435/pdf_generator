# Bank Statement Layout Migration

Frontend React + Tailwind becomes the **source of truth for layout**. Backend keeps **data generation, validation, metadata, encryption, and download**.

## Phase 1 — Code Audit

### Current PDF flow (unchanged for download)

```
StatementService.previewPdf / downloadPdf
  → PdfGeneratorService.renderPdf()
    → BankPdfService (SBI / KOTAK / BOI) — iText 7 layout
    → OR renderHtmlPdf() — Thymeleaf + OpenHTMLtoPDF (other banks)
```

### Backend layout code (to deprecate bank-by-bank)

| Bank  | Layout service              | Styles              | Icons              | Assets                          |
|-------|-----------------------------|---------------------|--------------------|---------------------------------|
| Kotak | `KotakPdfService.java`      | `KotakPdfStyles`    | `KotakPdfIcons` (FA codepoints) | `resources/pdf/kotak/*`         |
| SBI   | `SbiPdfService.java`        | `SbiPdfStyles`      | `SbiPdfIcons` (FA) | `resources/pdf/sbiLogo.png`     |
| BOI   | `BoiPdfService.java`        | `BoiPdfStyles`      | none (text layout) | `resources/pdf/boi/boi-logo.png`|

Supporting backend-only files: `KotakPageEventHandler`, `KotakPageContext`, `KotakAdBannerImage`, `KotakNarrationGenerator`, `BoiPdfEncryptor`, `BoiItext5Finalizer`, `BoiPdfPassword`.

### Frontend (new — layout source of truth)

```
frontend/src/templates/
  shared/
    tokens/     kotak.ts | sbi.ts | boi.ts
    components/ BankHeader, CustomerInfoSection, TransactionTable, Footer, ...
  kotak/KotakStatement.tsx   ← Phase 2 primary (most complete)
  sbi/SBIStatement.tsx       ← scaffold
  boi/BOIStatement.tsx       ← scaffold
  BankStatementPreview.tsx   ← router by bankCode
```

### Duplicated logic

- **Date/amount formatting**: backend `PdfGeneratorService` + per-bank services ↔ frontend `formatters.ts` + `statementViewModel.ts`
- **Logos**: backend `resources/pdf/*` ↔ frontend `public/*` (frontend is canonical going forward)
- **Thymeleaf templates** (`templates/banks/*/statement.html`): bypassed for SBI/KOTAK/BOI; only used for HDFC/ICICI/etc.

### What must NOT break

- `POST /api/statements/{id}/generate-transactions` — transaction + narration generation
- `POST /api/statements/{id}/preview` — legacy iText preview
- `POST /api/statements/{id}/download` — legacy iText + **BOI password encryption**
- BOI password: `BoiPdfPassword` + `BoiPdfEncryptor` on download only

---

## Phase 2 — Implemented (this PR)

### New API (additive only)

```
POST /api/statements/{id}/preview-layout
Body: { "html": "<!DOCTYPE html>..." }
Response: application/pdf (OpenHTMLtoPDF, unencrypted)
```

- `LayoutPdfRequest.java`
- `HtmlPdfRendererService.java`
- `StatementService.previewLayoutPdf()`

### Frontend preview

- `StatementDetailPage` — tabs: **Layout Preview** (React) | **Legacy PDF** (iframe)
- **Preview Layout PDF** — captures DOM via `captureStatementHtml()` → `preview-layout` endpoint
- **Preview Legacy PDF** / **Download** — unchanged

### Files created

| Path | Purpose |
|------|---------|
| `frontend/src/templates/shared/tokens/*.ts` | Bank design tokens |
| `frontend/src/templates/shared/components/*` | Reusable layout blocks |
| `frontend/src/templates/kotak/KotakStatement.tsx` | Kotak layout (page 1 + txn table) |
| `frontend/src/templates/sbi/SBIStatement.tsx` | SBI scaffold |
| `frontend/src/templates/boi/BOIStatement.tsx` | BOI scaffold |
| `frontend/src/lib/statementViewModel.ts` | API → template view model |
| `frontend/src/lib/exportStatementHtml.ts` | HTML snapshot for PDF export |
| `backend/.../HtmlPdfRendererService.java` | HTML → PDF |
| `backend/.../LayoutPdfRequest.java` | Request DTO |

### Files deprecated (not removed yet)

| File | When to remove |
|------|----------------|
| `KotakPdfService.java` (layout sections) | After Kotak HTML export matches pixel-perfect |
| `SbiPdfService.java` | After SBI migration verified |
| `BoiPdfService.java` (layout only) | After BOI migration; keep encryptor |
| `KotakPdfIcons.java`, `SbiPdfIcons.java` | After icons moved to Lucide/SVG |
| Thymeleaf `templates/banks/{sbi,kotak}/statement.html` | Unused for these banks |

---

## Icon mapping (Font Awesome → Lucide / SVG)

| Backend (FA)     | Frontend replacement        | Used in              |
|------------------|----------------------------|----------------------|
| PHONE            | `Phone` (lucide-react)     | Kotak assistance     |
| BRANCH           | `Building2` or `/icons/branch.svg` | Kotak assistance |
| CALENDAR         | `Calendar`                 | SBI customer block   |
| USER             | `User`                     | SBI                  |
| ENVELOPE         | `Mail`                     | SBI                  |
| LOCATION / MapPin| `MapPin`                   | SBI                  |
| CREDIT_CARD      | `CreditCard`               | SBI                  |
| BANK             | `Landmark`                 | SBI                  |
| BUILDING         | `Building2`                | SBI branch           |
| FILE             | `FileText`                 | SBI                  |
| USER_PLUS        | `UserPlus`                 | SBI nominee          |
| INFO             | `Info`                     | InstructionBox ✓     |

Prefer `lucide-react`. If visual mismatch, add SVG under `public/icons/` and use the same asset in preview and export.

---

## Rollout plan (bank by bank)

### Kotak (in progress)

1. ✅ Tokens + shared components + page-1 layout
2. ☐ Summary pages, AdKotak banner, assistance/Remember sections, continuation header
3. ☐ Compare Layout PDF vs Legacy PDF side-by-side
4. ☐ Switch `download` to HTML path + Kotak metadata post-process (optional)
5. ☐ Remove Kotak iText layout code

### SBI (next)

1. ✅ Scaffold template
2. ☐ Port customer blocks, icon rows, multi-page footer from `SbiPdfService`
3. ☐ Tune `sbi.ts` tokens against reference PDF

### BOI (last — password sensitive)

1. ✅ Scaffold + logo in `public/`
2. ☐ Full layout parity with `BoiPdfService`
3. ☐ Wire download: HTML → PDF → `BoiPdfEncryptor.encrypt()` (keep password logic)
4. ☐ Never encrypt preview; only download

---

## Safe rollout checklist

- [ ] Generate transactions — amounts/narrations unchanged
- [ ] Legacy preview still works for all banks
- [ ] Download still works; BOI password opens downloaded file
- [ ] Layout preview renders for SBI/KOTAK/BOI
- [ ] Layout PDF export returns readable PDF
- [ ] No duplicate logos required in backend after cutover

---

## Developer notes

- **Preview uses React** (`BankStatementPreview`); **download still uses iText** until cutover per bank.
- Tailwind classes in templates are compiled at build time; exported HTML includes inline styles from components.
- `captureStatementHtml` rewrites `/public` image paths to absolute URLs for OpenHTMLtoPDF.
- Clari5 design system applies to app chrome (`StatementDetailPage` actions/cards), not print templates.
